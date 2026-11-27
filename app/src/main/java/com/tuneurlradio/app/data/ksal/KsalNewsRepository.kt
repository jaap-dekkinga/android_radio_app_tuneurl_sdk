package com.tuneurlradio.app.data.ksal

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** One ksal.com story, built from the WordPress REST API. */
data class NewsArticle(
    val id: Int,
    val title: String,
    val excerpt: String,
    val contentHtml: String,
    val date: Date?,
    val author: String?,
    val imageUrl: String?,
    val thumbnailUrl: String?,
    val categoryName: String?,
    val link: String?
)

data class NewsPage(
    val articles: List<NewsArticle>,
    val totalPages: Int?,
    /** WordPress answers 400 for a page past the end. */
    val pastEnd: Boolean
)

/**
 * ksal.com posts from the WordPress REST API (replaces the RSS feed).
 * Port of iOS NewsFeed.swift / NewsModels.swift.
 */
@Singleton
class KsalNewsRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    /** Every article loaded so far, so the article screen can open by id. */
    private val cache = ConcurrentHashMap<Int, NewsArticle>()

    fun cached(id: Int): NewsArticle? = cache[id]

    suspend fun fetchPage(
        category: Ksal.Category,
        page: Int,
        search: String? = null,
        pageSize: Int = 20
    ): Result<NewsPage> = withContext(Dispatchers.IO) {
        try {
            val url = Ksal.POSTS_API.toHttpUrl().newBuilder()
                .addQueryParameter("per_page", pageSize.toString())
                .addQueryParameter("page", page.toString())
                .addQueryParameter("_embed", "author,wp:featuredmedia,wp:term")
                .apply {
                    category.wordpressId?.let { addQueryParameter("categories", it.toString()) }
                    if (!search.isNullOrBlank()) addQueryParameter("search", search)
                }
                .build()

            okHttpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                val totalPages = response.header("X-WP-TotalPages")?.toIntOrNull()
                if (response.code == 400) {
                    return@withContext Result.success(NewsPage(emptyList(), totalPages, pastEnd = true))
                }
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("ksal.com answered ${response.code}"))
                }
                val body = response.body?.string().orEmpty()
                val posts = JsonParser.parseString(body).asJsonArray
                val articles = posts.mapNotNull { runCatching { parsePost(it.asJsonObject) }.getOrNull() }
                articles.forEach { cache[it.id] = it }
                Result.success(NewsPage(articles, totalPages, pastEnd = false))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "News request failed (${category.id}, page $page)", e)
            Result.failure(e)
        }
    }

    private fun parsePost(o: JsonObject): NewsArticle {
        val embedded = o.obj("_embedded")
        val media = embedded?.arr("wp:featuredmedia")?.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
        // WordPress sends `sizes` as [] when empty, so read leniently.
        val sizes = media?.obj("media_details")?.get("sizes")?.takeIf { it.isJsonObject }?.asJsonObject
        fun size(name: String) = sizes?.obj(name)?.str("source_url")
        val source = media?.str("source_url")
        val thumb = size("medium") ?: size("medium_large") ?: source
        val large = size("large") ?: size("medium_large") ?: source

        val category = embedded?.arr("wp:term")
            ?.flatMap { group -> if (group.isJsonArray) group.asJsonArray.toList() else emptyList() }
            ?.mapNotNull { it.takeIf { t -> t.isJsonObject }?.asJsonObject }
            ?.firstOrNull { it.str("taxonomy") == "category" }
            ?.str("name")

        val author = embedded?.arr("author")?.firstOrNull()
            ?.takeIf { it.isJsonObject }?.asJsonObject?.str("name")

        return NewsArticle(
            id = o.get("id").asInt,
            title = Html.strip(o.obj("title")?.str("rendered").orEmpty()),
            excerpt = Html.strip(o.obj("excerpt")?.str("rendered").orEmpty()),
            contentHtml = o.obj("content")?.str("rendered").orEmpty(),
            date = o.str("date_gmt")?.let { parseGmt(it) },
            author = author,
            imageUrl = large,
            thumbnailUrl = thumb,
            categoryName = category?.let { Html.strip(it) },
            link = o.str("link")
        )
    }

    private fun parseGmt(value: String): Date? = runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(value)
    }.getOrNull()

    private companion object {
        const val TAG = "KsalNews"
    }
}

internal fun JsonObject.obj(key: String): JsonObject? =
    get(key)?.takeIf { it.isJsonObject }?.asJsonObject

internal fun JsonObject.arr(key: String): JsonArray? =
    get(key)?.takeIf { it.isJsonArray }?.asJsonArray

internal fun JsonObject.str(key: String): String? =
    get(key)?.takeIf { it.isJsonPrimitive }?.asString

/** Tag stripping and entity decoding for WordPress titles and excerpts. */
object Html {
    private val named = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "hellip" to "…", "ndash" to "–", "mdash" to "—", "lsquo" to "‘", "rsquo" to "’",
        "ldquo" to "“", "rdquo" to "”", "copy" to "©", "reg" to "®", "deg" to "°",
    )
    private val entity = Regex("&(#[xX][0-9a-fA-F]{1,6}|#[0-9]{1,7}|[a-zA-Z]{2,8});")

    fun strip(html: String): String {
        var text = html.replace(Regex("<[^>]+>"), "")
        text = decodeEntities(text)
        text = text.replace(Regex("[ \\t\\n\\r]+"), " ")
        text = text.replace("[…] ", "…").replace(" […]", "…")
        return text.trim()
    }

    fun decodeEntities(text: String): String {
        if (!text.contains('&')) return text
        return entity.replace(text) { m ->
            val e = m.groupValues[1]
            val decoded = when {
                e.startsWith("#x") || e.startsWith("#X") -> e.substring(2).toIntOrNull(16)?.let { cp(it) }
                e.startsWith("#") -> e.substring(1).toIntOrNull()?.let { cp(it) }
                else -> named[e]
            }
            decoded ?: m.value
        }
    }

    private fun cp(code: Int): String? =
        if (Character.isValidCodePoint(code)) String(Character.toChars(code)) else null
}
