package com.hindigk.audio.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Blogger (blogspot) se posts aur unke audio links laata hai.
 * Feed JSON: https://<host>/feeds/posts/default?alt=json
 */
object BloggerRepo {

    private const val TAG = "BloggerRepo"
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val audioRe: Pattern = Pattern.compile(
        "<audio\\b[^>]*src\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE
    )
    private val figRe: Pattern = Pattern.compile(
        "<figcaption[^>]*>([\\s\\S]*?)</figcaption>", Pattern.CASE_INSENSITIVE
    )
    private val tagRe: Pattern = Pattern.compile("<[^>]*>")

    /** Network se live feed laata hai (naya post turant dikhta hai). */
    suspend fun fetchBlog(blog: Blog): List<BlogPost> = withContext(Dispatchers.IO) {
        val out = ArrayList<BlogPost>()
        val step = 100
        var start = 1
        var total = Int.MAX_VALUE
        var guard = 0
        while (out.size < total && guard < 6) {
            guard++
            val url = "https://${blog.host}/feeds/posts/default?alt=json&start-index=$start&max-results=$step"
            val body = httpGet(url)
            if (body.isNullOrBlank()) break
            val json = JSONObject(body)
            total = json.optJSONObject("openSearch\$totalResults")
                ?.optString("\$t")?.toIntOrNull() ?: out.size
            val feed = json.optJSONObject("feed") ?: break
            val entries = feed.optJSONArray("entry")
            if (entries == null || entries.length() == 0) break
            for (i in 0 until entries.length()) {
                parseEntry(entries.optJSONObject(i) ?: continue)?.let { out.add(it) }
            }
            if (entries.length() < step) break
            start += step
        }
        out
    }

    private fun httpGet(url: String): String? = try {
        client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }
    } catch (e: Exception) {
        Log.e(TAG, "httpGet failed: $url", e)
        null
    }

    private fun parseEntry(e: JSONObject): BlogPost? {
        val title = Utils.unescape(e.optJSONObject("title")?.optString("\$t") ?: return null)
        val published = e.optJSONObject("published")?.optString("\$t").orEmpty()
        val content = e.optJSONObject("content")?.optString("\$t").orEmpty()
        val url = findAlternateLink(e)
        val tracks = extractTracks(content)
        return BlogPost(title, published, tracks, url)
    }

    private fun findAlternateLink(e: JSONObject): String {
        val links = e.optJSONArray("link") ?: return ""
        for (i in 0 until links.length()) {
            val l = links.optJSONObject(i) ?: continue
            if (l.optString("rel") == "alternate") return l.optString("href")
        }
        return ""
    }

    /** Post ke HTML content se saare <audio src="...mp3"> nikalta hai (order me, caption ke naam ke saath). */
    private fun extractTracks(content: String): List<Track> {
        if (content.isEmpty()) return emptyList()
        val tracks = ArrayList<Track>()
        val am = audioRe.matcher(content)
        var lastEnd = 0
        while (am.find()) {
            val src = am.group(1)?.trim().orEmpty()
            if (src.isNotBlank()) {
                val before = content.substring(lastEnd, am.start())
                var name = ""
                val fm = figRe.matcher(before)
                var lastCaption: String? = null
                while (fm.find()) {
                    name = Utils.stripHtml(fm.group(1) ?: "").trim()
                    lastCaption = name
                }
                val idx = tracks.size + 1
                val finalName = if (name.isNullOrBlank() || isGenericCaption(lastCaption))
                    "भाग $idx" else name
                tracks.add(Track(finalName, src))
            }
            lastEnd = am.end()
        }
        return tracks
    }

    // "Listen to the the audio file" jaisi generic caption ko bhag-number se replace karo
    private val genericSet = setOf(
        "listen to the the audio file", "listen to the audio file",
        "your browser does not support the audio element"
    )

    private fun isGenericCaption(c: String?): Boolean {
        if (c == null) return true
        val lower = c.lowercase().trim()
        if (lower.isEmpty()) return true
        genericSet.forEach { if (lower == it) return true }
        return false
    }

    // ---------- Simple disk cache: pehli bar instant dikhega ----------

    fun cacheFile(ctx: Context, blog: Blog): File =
        File(ctx.cacheDir, "feed_${blog.host}.json")

    fun saveCache(ctx: Context, blog: Blog, posts: List<BlogPost>) {
        try {
            val arr = JSONArray()
            posts.forEach { p ->
                val o = JSONObject()
                o.put("t", p.title)
                o.put("p", p.published)
                o.put("u", p.url)
                val names = JSONArray(); val urls = JSONArray()
                p.tracks.forEach { names.put(it.name); urls.put(it.url) }
                o.put("n", names); o.put("a", urls)
                arr.put(o)
            }
            cacheFile(ctx, blog).writeText(arr.toString())
        } catch (e: Exception) {
            Log.w(TAG, "cache save failed", e)
        }
    }

    fun getCached(ctx: Context, blog: Blog): List<BlogPost>? = try {
        val f = cacheFile(ctx, blog)
        if (f.exists()) {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val names = o.optJSONArray("n"); val urls = o.optJSONArray("a")
                val tracks = ArrayList<Track>()
                if (names != null && urls != null) {
                    for (j in 0 until minOf(names.length(), urls.length())) {
                        val u = urls.optString(j)
                        if (u.startsWith("http")) tracks.add(Track(names.optString(j), u))
                    }
                }
                BlogPost(o.optString("t"), o.optString("p"), tracks, o.optString("u"))
            }
        } else null
    } catch (e: Exception) {
        Log.w(TAG, "cache read failed", e)
        null
    }
}
