package com.hindigk.audio.audio

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.hindigk.audio.data.BlogPost
import com.hindigk.audio.data.Track

/**
 * App-wide single MediaController (PlayerService se connect hota hai).
 * Sab UI iske through aur/next/prev seek sab control karta hai.
 */
object PlayerManager {

    private const val TAG = "PlayerManager"

    @Volatile
    var controller: MediaController? = null
        private set

    private var future: ListenableFuture<MediaController>? = null

    /** Abhi kun si post queue me hai (mini player se PlayerActivity khulne ke liye). */
    @Volatile
    var currentPost: BlogPost? = null

    fun connect(context: Context, onConnected: (MediaController) -> Unit) {
        val app = context.applicationContext
        controller?.let { onConnected(it); return }
        synchronized(this) {
            if (future == null) {
                try {
                    val token = SessionToken(app, ComponentName(app, PlayerService::class.java))
                    future = MediaController.Builder(app, token).buildAsync()
                } catch (e: Exception) {
                    Log.e(TAG, "connect failed", e)
                    future = null
                    return
                }
            }
        }
        val f = future ?: return
        val mainExecutor = java.util.concurrent.Executor { r -> Handler(Looper.getMainLooper()).post(r) }
        mainExecutor.execute {
            try {
                if (controller == null) controller = f.get()
            } catch (e: Exception) {
                Log.e(TAG, "controller creation failed", e)
                synchronized(this) { future = null }
                return@execute
            }
            controller?.let(onConnected)
        }
    }

    /** Ek post ke saare tracks ko queue me daal kar startIndex se play karta hai. */
    fun play(post: BlogPost, startIndex: Int) {
        if (post.tracks.isEmpty() || startIndex !in post.tracks.indices) return
        currentPost = post
        withController { c ->
            val items = post.tracks.map { t ->
                MediaItem.Builder()
                    .setUri(t.url)
                    .setMediaId(t.url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(t.name)
                            .setArtist(post.title)
                            .build()
                    )
                    .build()
            }
            c.setMediaItems(items, startIndex, 0L)
            c.prepare()
            c.play()
        }
    }

    fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            try { block(c) } catch (e: Exception) { Log.e(TAG, "action failed", e) }
        } else {
            // controller abhi connect nahi hua — pending rakh do, connect ho te hi chalega
            handleOnUiThread { c2 -> try { block(c2) } catch (e: Exception) { Log.e(TAG, "action failed", e) } }
        }
    }

    private fun handleOnUiThread(block: (MediaController) -> Unit) {
        val ctx = lastContext ?: return
        connect(ctx, block)
    }

    @Volatile
    private var lastContext: Context? = null

    /** Har UI se pehle context bhej do, taaki connect ho sake. */
    fun attach(context: Context) { lastContext = context.applicationContext }

    fun togglePlayPause() = withController { c ->
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = withController { c -> if (c.hasNextMediaItem()) c.seekToNextMediaItem() }

    fun prev() = withController { c ->
        if (c.hasPreviousMediaItem()) c.seekToPreviousMediaItem()
        else c.seekTo(0)
    }

    fun seekTo(ms: Long) = withController { c -> c.seekTo(ms) }

    fun setSpeed(speed: Float) = withController { c ->
        c.playbackParameters = androidx.media3.common.PlaybackParameters(speed)
    }
}
