package com.storyboy.data

import com.storyboy.core.SupabaseConfig
import java.net.HttpURLConnection
import java.net.URL

class SupabaseHttpException(val statusCode: Int, message: String) : Exception(message)

object SupabaseApi {
    fun get(path: String, accessToken: String? = null): String {
        return request(method = "GET", path = path, body = null, accessToken = accessToken)
    }

    fun post(path: String, body: String, accessToken: String? = null, prefer: String? = null): String {
        return request(method = "POST", path = path, body = body, accessToken = accessToken, prefer = prefer)
    }

    fun put(path: String, body: String, accessToken: String? = null): String {
        return request(method = "PUT", path = path, body = body, accessToken = accessToken)
    }

    private fun request(
        method: String,
        path: String,
        body: String?,
        accessToken: String?,
        prefer: String? = null,
    ): String {
        val connection = URL("${SupabaseConfig.Url}$path").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.requestMethod = method
            connection.setRequestProperty("apikey", SupabaseConfig.PublishableKey)
            connection.setRequestProperty("Authorization", "Bearer ${accessToken ?: SupabaseConfig.PublishableKey}")
            connection.setRequestProperty("Accept", "application/json")
            prefer?.let { connection.setRequestProperty("Prefer", it) }
            if (body != null) {
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.bufferedWriter().use { it.write(body) }
            }

            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }
                .orEmpty()
            if (status !in 200..299) {
                throw SupabaseHttpException(status, extractErrorMessage(text, status))
            }
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun extractErrorMessage(body: String, status: Int): String {
        return runCatching {
            val json = org.json.JSONObject(body)
            listOf("msg", "message", "error_description", "error")
                .firstNotNullOfOrNull { key -> json.optString(key).takeIf { it.isNotBlank() } }
        }.getOrNull() ?: "Request failed with HTTP $status"
    }
}
