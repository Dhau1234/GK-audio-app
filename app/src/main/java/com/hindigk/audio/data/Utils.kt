package com.hindigk.audio.data

object Utils {

    private val HTML_ENTITIES = mapOf(
        "&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"",
        "&#39;" to "'", "&apos;" to "'", "&nbsp;" to " "
    )

    fun unescape(s: String): String {
        var r = s
        HTML_ENTITIES.forEach { (k, v) -> r = r.replace(k, v) }
        r = r.replace(Regex("&#(\\d+);")) { m -> (m.groupValues[1].toIntOrNull() ?: 0).toChar().toString() }
        return r.trim()
    }

    private val tagRe = Regex("<[^>]*>")

    /** HTML fragment se tags hatata hai (figcaption ke liye). */
    fun stripHtml(s: String): String {
        var r = unescape(tagRe.replace(s, " "))
        r = r.replace(Regex("\\s+"), " ")
        return r.trim()
    }

    /** "2026-10-14T02:34:06.716-07:00" → "14 Oct 2026" */
    fun formatDate(iso: String): String {
        if (iso.length < 10) return ""
        return try {
            val p = iso.substring(0, 10).split("-")
            val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val m = months.getOrNull((p.getOrNull(1)?.toIntOrNull() ?: 1) - 1) ?: ""
            "${p.getOrNull(2)?.toIntOrNull() ?: ""} $m ${p.getOrNull(0) ?: ""}"
        } catch (e: Exception) {
            ""
        }
    }

    fun fmtTime(ms: Long): String {
        if (ms < 0) return "0:00"
        val totalSec = ms / 1000
        return String.format("%d:%02d", totalSec / 60, totalSec % 60)
    }
}
