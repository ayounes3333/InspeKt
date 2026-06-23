package com.inspekt.util

import com.inspekt.domain.model.Environment
import com.inspekt.domain.model.HttpRequest
import com.inspekt.domain.model.KeyValueParam
import com.inspekt.domain.model.RequestBody

object VariableInterpolator {

    private val VARIABLE_PATTERN = Regex("\\{\\{(.*?)}}")
    private const val MAX_DEPTH = 10

    fun interpolate(input: String, variables: Map<String, String>): String {
        var result = input
        repeat(MAX_DEPTH) {
            val previous = result
            result = VARIABLE_PATTERN.replace(result) { match ->
                val key = match.groupValues[1].trim()
                variables[key] ?: match.value
            }
            if (result == previous) return result
        }
        return result
    }

    fun resolveRequest(request: HttpRequest, environment: Environment?): HttpRequest {
        if (environment == null) return request

        val variables = environment.variables
            .filter { it.enabled && it.key.isNotBlank() }
            .associate { it.key to it.value }

        if (variables.isEmpty()) return request

        return request.copy(
            url = interpolate(request.url, variables),
            headers = request.headers.map { it.resolve(variables) },
            queryParams = request.queryParams.map { it.resolve(variables) },
            body = request.body.resolve(variables)
        )
    }

    private fun KeyValueParam.resolve(variables: Map<String, String>): KeyValueParam {
        return copy(
            key = interpolate(key, variables),
            value = interpolate(value, variables)
        )
    }

    private fun RequestBody.resolve(variables: Map<String, String>): RequestBody {
        return copy(
            rawContent = interpolate(rawContent, variables),
            formParams = formParams.map { it.resolve(variables) }
        )
    }
}
