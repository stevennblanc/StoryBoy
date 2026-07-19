package com.storyboy.repository

import com.storyboy.data.SupabaseApi
import com.storyboy.data.SupabaseHttpException
import com.storyboy.models.CatalogueBook
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads the rich store catalogue from the Supabase books table and manages
 * the signed-in user's purchases. All methods perform blocking network IO.
 */
class StoreCatalogueRepository {

    fun fetchCatalogue(): Map<String, CatalogueBook> {
        val response = SupabaseApi.get(
            path = "/rest/v1/books?select=*&is_published=eq.true",
        )
        val rows = JSONArray(response)
        return buildMap {
            for (index in 0 until rows.length()) {
                val book = rows.getJSONObject(index).toCatalogueBook()
                put(book.id, book)
            }
        }
    }

    fun fetchOwnedBookIds(accessToken: String): Set<String> {
        val response = SupabaseApi.get(
            path = "/rest/v1/purchases?select=book_id",
            accessToken = accessToken,
        )
        val rows = JSONArray(response)
        return buildSet {
            for (index in 0 until rows.length()) {
                add(rows.getJSONObject(index).getString("book_id"))
            }
        }
    }

    fun acquireBook(accessToken: String, userId: String, bookId: String) {
        try {
            SupabaseApi.post(
                path = "/rest/v1/purchases",
                body = JSONObject()
                    .put("user_id", userId)
                    .put("book_id", bookId)
                    .toString(),
                accessToken = accessToken,
                prefer = "return=minimal",
            )
        } catch (exception: SupabaseHttpException) {
            // 409: already owned; treat as success.
            if (exception.statusCode != 409) throw exception
        }
    }

    private fun JSONObject.toCatalogueBook(): CatalogueBook {
        return CatalogueBook(
            id = getString("id"),
            title = optString("title"),
            author = optString("author"),
            genre = optString("genre"),
            description = optString("description"),
            about = optStringOrEmpty("about"),
            version = optString("version"),
            priceUsd = optDouble("price_usd", 0.0),
            language = optStringOrEmpty("language"),
            publisher = optStringOrEmpty("publisher"),
            publishedOn = optStringOrEmpty("published_on"),
            nodeCount = optIntOrNull("node_count"),
            endingCount = optIntOrNull("ending_count"),
            fileSizeBytes = if (isNull("file_size_bytes")) null else optLong("file_size_bytes"),
            features = optJSONArray("features")?.let { features ->
                buildList {
                    for (index in 0 until features.length()) {
                        add(features.getString(index))
                    }
                }
            }.orEmpty(),
        )
    }

    private fun JSONObject.optStringOrEmpty(name: String): String {
        return if (isNull(name)) "" else optString(name)
    }

    private fun JSONObject.optIntOrNull(name: String): Int? {
        return if (isNull(name)) null else optInt(name)
    }
}
