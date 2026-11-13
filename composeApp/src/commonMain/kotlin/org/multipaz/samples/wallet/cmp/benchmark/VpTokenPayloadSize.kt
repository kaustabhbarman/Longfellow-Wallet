
package org.multipaz.samples.wallet.cmp.benchmark

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.multipaz.util.Logger
import org.multipaz.util.fromBase64Url

/**
 * Total decoded byte length of Base64URL-looking strings in the VP token JSON.
 */
fun measureVpTokenPayloadBytes(vpTokenJson: String): Long? {
    return runCatching {
        val root = Json.parseToJsonElement(vpTokenJson)
        sumDecodedBase64UrlBytes(root)
    }.getOrNull()
}

/**
 * Diagnostic only: logs JSON structure and string sizes, never string contents.
 */
fun logVpTokenJsonStructure(vpTokenJson: String) {
    try {
        val root = Json.parseToJsonElement(vpTokenJson)
        var stringCount = 0
        var candidateCount = 0
        var decodedTotal = 0L
        var loggedCount = 0

        fun inspect(element: JsonElement, path: String) {
            when (element) {
                is JsonObject -> {
                    Logger.i(
                        ZKP_BENCHMARK_LOG_TAG,
                        "VP structure: $path type=object fields=${element.size}"
                    )
                    element.forEach { (key, value) ->
                        inspect(value, "$path.$key")
                    }
                }

                is JsonArray -> {
                    Logger.i(
                        ZKP_BENCHMARK_LOG_TAG,
                        "VP structure: $path type=array items=${element.size}"
                    )
                    element.forEachIndexed { index, value ->
                        inspect(value, "$path[$index]")
                    }
                }

                is JsonPrimitive -> {
                    if (element.isString) {
                        stringCount++
                        val value = element.content
                        val candidate = looksLikeBase64UrlPayload(value)

                        var decodedBytes: Long? = null
                        if (candidate) {
                            candidateCount++
                            decodedBytes = runCatching {
                                value.fromBase64Url().size.toLong()
                            }.getOrNull()

                            if (decodedBytes != null) {
                                decodedTotal += decodedBytes
                            }
                        }

                        if (loggedCount < 100) {
                            Logger.i(
                                ZKP_BENCHMARK_LOG_TAG,
                                "VP string: path=$path chars=${value.length} " +
                                    "base64UrlCandidate=$candidate " +
                                    "decodedBytes=${decodedBytes ?: "n/a"}"
                            )
                            loggedCount++
                        }
                    } else {
                        Logger.i(
                            ZKP_BENCHMARK_LOG_TAG,
                            "VP structure: $path type=non-string"
                        )
                    }
                }
            }
        }

        inspect(root, "$")

        Logger.i(
            ZKP_BENCHMARK_LOG_TAG,
            "VP structure summary: strings=$stringCount " +
                "candidates=$candidateCount decodedTotal=$decodedTotal " +
                "detailsLogged=$loggedCount"
        )
    } catch (error: Exception) {
        Logger.w(
            ZKP_BENCHMARK_LOG_TAG,
            "VP structure inspection failed: ${error::class.simpleName}"
        )
    }
}

private fun sumDecodedBase64UrlBytes(element: JsonElement): Long = when (element) {
    is JsonObject -> element.values.sumOf { sumDecodedBase64UrlBytes(it) }
    is JsonArray -> element.sumOf { sumDecodedBase64UrlBytes(it) }
    is JsonPrimitive -> {
        if (!element.isString) {
            0L
        } else {
            val text = element.content
            if (looksLikeBase64UrlPayload(text)) {
                text.fromBase64Url().size.toLong()
            } else {
                0L
            }
        }
    }
}

private fun looksLikeBase64UrlPayload(value: String): Boolean {
    if (value.length < 16) return false
    return value.all { ch ->
        ch.isLetterOrDigit() || ch == '-' || ch == '_'
    }
}
