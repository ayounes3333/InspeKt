package com.inspekt.util

import com.inspekt.domain.model.*

/**
 * Parses a cURL command string into an [HttpRequest].
 *
 * Supports:
 *  -X / --request METHOD
 *  -H / --header "Key: Value"
 *  -d / --data / --data-raw / --data-binary / --data-ascii BODY
 *  --data-urlencode BODY
 *  -F / --form KEY=VALUE
 *  -u / --user user:pass  (converted to Authorization header)
 *  -b / --cookie COOKIE   (converted to Cookie header)
 *  -L / --location        (ignored, for display only)
 *  URLs with or without quotes
 */
object CurlParser {

    fun parse(curlCommand: String): Result<HttpRequest> = runCatching {
        val tokens = tokenize(curlCommand.trim())
        require(tokens.isNotEmpty()) { "Empty cURL command" }

        // Skip the leading "curl" token
        val args = if (tokens[0].equals("curl", ignoreCase = true)) tokens.drop(1) else tokens

        var url = ""
        var method: HttpMethod? = null
        val headers = mutableListOf<KeyValueParam>()
        var bodyContent = ""
        var bodyType = BodyType.NONE
        val formParams = mutableListOf<KeyValueParam>()

        var i = 0
        while (i < args.size) {
            val arg = args[i]
            when {
                // URL (bare argument not starting with -)
                !arg.startsWith("-") -> {
                    url = arg.trim('\'', '"')
                    i++
                }

                // Method
                arg == "-X" || arg == "--request" -> {
                    method = parseMethod(args.getOrNull(++i) ?: "GET")
                    i++
                }

                // Headers
                arg == "-H" || arg == "--header" -> {
                    val headerStr = args.getOrNull(++i) ?: ""
                    val colon = headerStr.indexOf(':')
                    if (colon > 0) {
                        val key = headerStr.substring(0, colon).trim()
                        val value = headerStr.substring(colon + 1).trim()
                        headers.add(KeyValueParam(key = key, value = value))
                    }
                    i++
                }

                // Data body (raw JSON, form string, etc.)
                arg == "-d" || arg == "--data" || arg == "--data-raw" ||
                arg == "--data-binary" || arg == "--data-ascii" -> {
                    val data = args.getOrNull(++i) ?: ""
                    bodyContent = data
                    // Detect JSON vs form-encoded
                    bodyType = when {
                        data.trimStart().startsWith("{") || data.trimStart().startsWith("[") -> BodyType.JSON
                        else -> BodyType.FORM_URL_ENCODED
                    }
                    if (method == null) method = HttpMethod.POST
                    i++
                }

                // URL-encoded data
                arg == "--data-urlencode" -> {
                    val data = args.getOrNull(++i) ?: ""
                    bodyContent = if (bodyContent.isEmpty()) data else "$bodyContent&$data"
                    bodyType = BodyType.FORM_URL_ENCODED
                    if (method == null) method = HttpMethod.POST
                    i++
                }

                // Multipart form fields
                arg == "-F" || arg == "--form" -> {
                    val field = args.getOrNull(++i) ?: ""
                    val eq = field.indexOf('=')
                    if (eq > 0) {
                        formParams.add(
                            KeyValueParam(
                                key = field.substring(0, eq).trim(),
                                value = field.substring(eq + 1).trim('@', '"', '\'')
                            )
                        )
                    }
                    bodyType = BodyType.MULTIPART_FORM
                    if (method == null) method = HttpMethod.POST
                    i++
                }

                // Basic auth → Authorization header
                arg == "-u" || arg == "--user" -> {
                    val creds = args.getOrNull(++i) ?: ""
                    val encoded = creds.encodeBase64()
                    headers.add(KeyValueParam(key = "Authorization", value = "Basic $encoded"))
                    i++
                }

                // Cookie → Cookie header
                arg == "-b" || arg == "--cookie" -> {
                    val cookie = args.getOrNull(++i) ?: ""
                    headers.add(KeyValueParam(key = "Cookie", value = cookie))
                    i++
                }

                // User-Agent
                arg == "-A" || arg == "--user-agent" -> {
                    val ua = args.getOrNull(++i) ?: ""
                    headers.add(KeyValueParam(key = "User-Agent", value = ua))
                    i++
                }

                // Skip flags we don't use
                else -> i++
            }
        }

        // Infer JSON body type from Content-Type header if present
        if (bodyType == BodyType.FORM_URL_ENCODED &&
            headers.any { it.key.equals("Content-Type", true) &&
                    it.value.contains("application/json", true) }
        ) {
            bodyType = BodyType.JSON
        }

        // Parse query params out of URL
        val (cleanUrl, queryParams) = extractQueryParams(url)

        val requestBody = when (bodyType) {
            BodyType.NONE -> RequestBody(BodyType.NONE)
            BodyType.MULTIPART_FORM -> RequestBody(BodyType.MULTIPART_FORM, formParams = formParams)
            BodyType.FORM_URL_ENCODED -> {
                // If bodyContent looks like key=value pairs, parse into form params
                val params = parseFormBody(bodyContent)
                if (params.isNotEmpty()) {
                    RequestBody(BodyType.FORM_URL_ENCODED, formParams = params)
                } else {
                    RequestBody(BodyType.FORM_URL_ENCODED, rawContent = bodyContent)
                }
            }
            else -> RequestBody(bodyType, rawContent = bodyContent)
        }

        HttpRequest(
            name = "Imported from cURL",
            method = method ?: HttpMethod.GET,
            url = cleanUrl,
            headers = headers,
            queryParams = queryParams,
            body = requestBody
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Tokenizes a shell-like command, respecting single/double quotes
     * and backslash line continuations.
     */
    private fun tokenize(input: String): List<String> {
        val cleaned = input.replace("\\\n", " ").replace("\\\\n", " ")
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var inSingle = false
        var inDouble = false
        var i = 0

        while (i < cleaned.length) {
            val c = cleaned[i]
            when {
                c == '\'' && !inDouble -> { inSingle = !inSingle; i++ }
                c == '"' && !inSingle -> { inDouble = !inDouble; i++ }
                c == '\\' && (inDouble || !inSingle) && i + 1 < cleaned.length -> {
                    current.append(cleaned[i + 1])
                    i += 2
                }
                (c == ' ' || c == '\t') && !inSingle && !inDouble -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                    i++
                }
                else -> { current.append(c); i++ }
            }
        }
        if (current.isNotEmpty()) tokens.add(current.toString())
        return tokens
    }

    private fun parseMethod(s: String): HttpMethod =
        HttpMethod.entries.firstOrNull { it.value.equals(s.trim(), ignoreCase = true) }
            ?: HttpMethod.GET

    private fun extractQueryParams(url: String): Pair<String, List<KeyValueParam>> {
        val qIdx = url.indexOf('?')
        if (qIdx < 0) return url to emptyList()

        val base = url.substring(0, qIdx)
        val query = url.substring(qIdx + 1)
        val params = query.split("&").mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq < 0) {
                if (part.isNotBlank()) KeyValueParam(key = urlDecode(part), value = "") else null
            } else {
                KeyValueParam(
                    key = urlDecode(part.substring(0, eq)),
                    value = urlDecode(part.substring(eq + 1))
                )
            }
        }
        return base to params
    }

    private fun parseFormBody(body: String): List<KeyValueParam> {
        if (!body.contains('=')) return emptyList()
        return body.split("&").mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq < 0) null
            else KeyValueParam(
                key = urlDecode(part.substring(0, eq).trim()),
                value = urlDecode(part.substring(eq + 1).trim())
            )
        }
    }

    /** Minimal URL percent-decoding. */
    private fun urlDecode(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            if (s[i] == '%' && i + 2 < s.length) {
                val hex = s.substring(i + 1, i + 3)
                val code = hex.toIntOrNull(16)
                if (code != null) {
                    sb.append(code.toChar())
                    i += 3
                    continue
                }
            }
            sb.append(s[i])
            i++
        }
        return sb.toString()
    }

    private fun String.encodeBase64(): String {
        val bytes = this.encodeToByteArray()
        val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val sb = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else 0
            sb.append(table[b0 shr 2])
            sb.append(table[((b0 and 3) shl 4) or (b1 shr 4)])
            sb.append(if (i + 1 < bytes.size) table[((b1 and 15) shl 2) or (b2 shr 6)] else '=')
            sb.append(if (i + 2 < bytes.size) table[b2 and 63] else '=')
            i += 3
        }
        return sb.toString()
    }
}
