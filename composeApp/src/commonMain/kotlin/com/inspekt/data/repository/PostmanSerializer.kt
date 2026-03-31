package com.inspekt.data.repository

import com.inspekt.data.model.*
import com.inspekt.domain.model.*
import com.inspekt.domain.model.Collection
import kotlinx.serialization.json.Json

/**
 * Converts between our internal domain models and Postman Collection v2.1 JSON format.
 */
object PostmanSerializer {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Export: domain → Postman JSON
    // ──────────────────────────────────────────────────────────────────────────

    fun exportCollection(collection: Collection): String {
        val postman = PostmanCollection(
            info = PostmanInfo(
                postmanId = collection.id,
                name = collection.name,
                description = collection.description
            ),
            item = collection.items.map { toPostmanItem(it) },
            variable = collection.variables.map { toPostmanVariable(it) }
        )
        return json.encodeToString(PostmanCollection.serializer(), postman)
    }

    private fun toPostmanItem(item: CollectionItem): PostmanItem = when (item) {
        is CollectionFolder -> PostmanItem(
            id = item.id,
            name = item.name,
            description = item.description,
            item = item.items.map { toPostmanItem(it) }
        )
        is CollectionRequest -> PostmanItem(
            id = item.id,
            name = item.name,
            description = item.description,
            request = toPostmanRequest(item.request)
        )
    }

    private fun toPostmanRequest(req: HttpRequest): PostmanRequest {
        val urlParts = parseUrl(req.url, req.queryParams)
        return PostmanRequest(
            method = req.method.value,
            header = req.headers
                .filter { it.key.isNotBlank() }
                .map { PostmanHeader(it.key, it.value, it.description, !it.enabled) },
            body = toPostmanBody(req.body),
            url = urlParts,
            description = req.description
        )
    }

    private fun parseUrl(rawUrl: String, queryParams: List<KeyValueParam>): PostmanUrl {
        val inlineQueries = queryParams
            .filter { it.key.isNotBlank() }
            .map { PostmanQuery(it.key, it.value, it.description, !it.enabled) }

        return PostmanUrl(
            raw = rawUrl,
            query = inlineQueries
        )
    }

    private fun toPostmanBody(body: RequestBody): PostmanBody? = when (body.type) {
        BodyType.NONE -> null
        BodyType.JSON -> PostmanBody(
            mode = "raw",
            raw = body.rawContent,
            options = PostmanBodyOptions(PostmanRawOptions("json"))
        )
        BodyType.RAW_TEXT -> PostmanBody(mode = "raw", raw = body.rawContent)
        BodyType.FORM_URL_ENCODED -> PostmanBody(
            mode = "urlencoded",
            urlencoded = body.formParams
                .filter { it.key.isNotBlank() }
                .map { PostmanFormParam(it.key, it.value, it.description, !it.enabled) }
        )
        BodyType.MULTIPART_FORM -> PostmanBody(
            mode = "formdata",
            formdata = body.formParams
                .filter { it.key.isNotBlank() }
                .map { PostmanFormParam(it.key, it.value, it.description, !it.enabled) }
        )
        BodyType.BINARY -> PostmanBody(mode = "file", raw = body.rawContent)
    }

    private fun toPostmanVariable(param: KeyValueParam) = PostmanVariable(
        id = param.id,
        key = param.key,
        value = param.value,
        description = param.description,
        disabled = !param.enabled
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Import: Postman JSON → domain
    // ──────────────────────────────────────────────────────────────────────────

    fun importCollection(jsonString: String): Result<Collection> = runCatching {
        val postman = json.decodeFromString(PostmanCollection.serializer(), jsonString)

        Collection(
            id = postman.info.postmanId.ifBlank { generateId() },
            name = postman.info.name,
            description = postman.info.description,
            items = postman.item.map { fromPostmanItem(it) },
            variables = postman.variable.map { fromPostmanVariable(it) }
        )
    }

    private fun fromPostmanItem(item: PostmanItem): CollectionItem {
        return if (item.request != null) {
            CollectionRequest(
                id = item.id.ifBlank { generateId() },
                name = item.name,
                description = item.description,
                request = fromPostmanRequest(item.request)
            )
        } else {
            CollectionFolder(
                id = item.id.ifBlank { generateId() },
                name = item.name,
                description = item.description,
                items = item.item?.map { fromPostmanItem(it) } ?: emptyList()
            )
        }
    }

    private fun fromPostmanRequest(req: PostmanRequest): HttpRequest {
        val method = HttpMethod.entries.firstOrNull {
            it.value.equals(req.method, ignoreCase = true)
        } ?: HttpMethod.GET

        return HttpRequest(
            id = generateId(),
            name = "",
            method = method,
            url = req.url.raw,
            headers = req.header.map {
                KeyValueParam(generateId(), it.key, it.value, it.description, !it.disabled)
            },
            queryParams = req.url.query.map {
                KeyValueParam(generateId(), it.key, it.value ?: "", it.description, !it.disabled)
            },
            body = fromPostmanBody(req.body),
            description = req.description
        )
    }

    private fun fromPostmanBody(body: PostmanBody?): RequestBody {
        if (body == null) return RequestBody(BodyType.NONE)
        return when (body.mode) {
            "raw" -> {
                val lang = body.options?.raw?.language ?: "text"
                val type = if (lang == "json") BodyType.JSON else BodyType.RAW_TEXT
                RequestBody(type, body.raw)
            }
            "urlencoded" -> RequestBody(
                type = BodyType.FORM_URL_ENCODED,
                formParams = body.urlencoded?.map {
                    KeyValueParam(generateId(), it.key, it.value, it.description, !it.disabled)
                } ?: emptyList()
            )
            "formdata" -> RequestBody(
                type = BodyType.MULTIPART_FORM,
                formParams = body.formdata?.map {
                    KeyValueParam(generateId(), it.key, it.value, it.description, !it.disabled)
                } ?: emptyList()
            )
            else -> RequestBody(BodyType.NONE)
        }
    }

    private fun fromPostmanVariable(v: PostmanVariable) = KeyValueParam(
        id = v.id.ifBlank { generateId() },
        key = v.key,
        value = v.value,
        description = v.description,
        enabled = !v.disabled
    )
}
