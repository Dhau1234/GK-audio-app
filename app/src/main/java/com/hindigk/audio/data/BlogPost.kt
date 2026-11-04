package com.hindigk.audio.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Track(
    val name: String,
    val url: String
) : Parcelable

@Parcelize
data class BlogPost(
    val title: String,
    val published: String,
    val tracks: List<Track>,
    val url: String
) : Parcelable {
    fun metaLine(): String = when {
        tracks.isEmpty() -> "ऑडियो जल्द ही आएगा"
        tracks.size == 1 -> "1 ऑडियो"
        else -> "${tracks.size} ऑडियो"
    }
}
