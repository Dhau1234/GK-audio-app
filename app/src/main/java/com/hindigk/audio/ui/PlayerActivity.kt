package com.hindigk.audio.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import com.hindigk.audio.R
import com.hindigk.audio.audio.PlayerManager
import com.hindigk.audio.data.BlogPost
import com.hindigk.audio.data.Track
import com.hindigk.audio.data.Utils
import com.hindigk.audio.databinding.ActivityPlayerBinding
import android.widget.Toast

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POST = "post"
        private val SPEEDS = floatArrayOf(1f, 1.25f, 1.5f, 1.75f, 2f)
    }

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var post: BlogPost
    private lateinit var adapter: TrackAdapter
    private var currentIdx = -1
    private var speedPos = 0
    private val handler = Handler(Looper.getMainLooper())
    private var listener: Player.Listener? = null

    private val ticker = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val p: BlogPost? = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_POST, BlogPost::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_POST)
        }
        if (p == null) { finish(); return }
        post = p

        binding.toolbar.title = post.title
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = TrackAdapter { track, index ->
            PlayerManager.play(post, index)
            highlight(index)
        }
        binding.rvTracks.layoutManager = LinearLayoutManager(this)
        binding.rvTracks.adapter = adapter
        adapter.submitList(post.tracks)

        if (post.tracks.isEmpty()) {
            Toast.makeText(this, R.string.no_audio, Toast.LENGTH_SHORT).show()
        }

        binding.btnPlay.setOnClickListener { PlayerManager.togglePlayPause() }
        binding.btnNext.setOnClickListener { PlayerManager.next() }
        binding.btnPrev.setOnClickListener { PlayerManager.prev() }
        binding.btnSpeed.setOnClickListener {
            speedPos = (speedPos + 1) % SPEEDS.size
            binding.btnSpeed.text = trimmedSpeed(SPEEDS[speedPos])
            PlayerManager.setSpeed(SPEEDS[speedPos])
        }
        binding.seek.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) { userSeeking = true }
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {
                userSeeking = false
                PlayerManager.seekTo(sb?.progress?.toLong() ?: 0L)
            }
        })

        PlayerManager.attach(this)
        connectController()
    }

    private var userSeeking = false

    private fun connectController() {
        PlayerManager.connect(this) { c ->
            listener = playerListener()
            try { c.addListener(listener!!) } catch (_: Exception) {}
            val sameQueue = PlayerManager.currentPost?.title == post.title
            if (sameQueue) {
                currentIdx = c.currentMediaItemIndex
                adapter.setPlaying(currentIdx)
            }
            runOnUiThread { refreshAll() }
            handler.post(ticker)
        }
    }

    private fun playerListener() = object : Player.Listener() {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            runOnUiThread { refreshAll() }
        }
        override fun onPlaybackStateChanged(playbackState: Int) { runOnUiThread { refreshAll() } }
        override fun onIsPlayingChanged(isPlaying: Boolean) { runOnUiThread { refreshAll() } }
        override fun onPositionDiscontinuity(
            oldPosition: androidx.media3.common.PositionInfo,
            newPosition: androidx.media3.common.PositionInfo,
            reason: Int
        ) { runOnUiThread { updateProgress() } }
        override fun onPlayerError(error: PlaybackException) {
            runOnUiThread { try { Toast.makeText(this@PlayerActivity, R.string.player_error, Toast.LENGTH_SHORT).show() } catch (_: Exception) {} }
        }
        override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
            val s = SPEEDS.indexOfFirst { Math.abs(it - playbackParameters.speed) < 0.001f }
            if (s >= 0) { speedPos = s; runOnUiThread { binding.btnSpeed.text = trimmedSpeed(SPEEDS[s]) } }
        }
    }

    private fun trimmedSpeed(v: Float): String =
        if (v == (v.toLong()).toFloat()) "${v.toLong()}.0×" else "${v}×"

    private fun refreshAll() {
        val c = PlayerManager.controller ?: return
        if (c.mediaItemCount == 0) {
            binding.controlCard.visibility = View.VISIBLE
            adapter.setPlaying(-1)
            currentIdx = -1
            binding.tvNow.text = ""
            binding.btnPlay.setImageResource(R.drawable.ic_play_arrow)
            return
        }
        binding.tvNow.text = if (sameQueue()) c.mediaMetadata.title ?: "" else ""
        binding.btnPlay.setImageResource(if (c.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow)
        if (sameQueue()) {
            if (c.currentMediaItemIndex != currentIdx) highlight(c.currentMediaItemIndex)
        }
        updateProgress()
    }

    private fun sameQueue(): Boolean = PlayerManager.currentPost?.title == post.title

    @SuppressLint("SetTextI18n")
    private fun updateProgress() {
        val c = PlayerManager.controller ?: return
        if (!sameQueue() || c.mediaItemCount == 0) { this.binding.progressBuffer.visibility = View.GONE; return }
        val dur = c.duration
        val pos = c.currentPosition
        if (dur != C.TIME_UNSET && dur > 0) {
            if (c.playbackState == Player.STATE_ENDED) {
                binding.seek.max = dur.toInt(); binding.seek.progress = dur.toInt()
            } else if (!userSeeking) {
                binding.seek.max = dur.toInt()
                binding.seek.progress = pos.toInt()
            }
            binding.tvDur.text = Utils.fmtTime(dur)
        } else {
            binding.tvDur.text = "--:--"
        }
        binding.tvCur.text = Utils.fmtTime(pos)
        binding.progressBuffer.visibility =
            if (c.playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
    }

    private fun highlight(index: Int) {
        if (index == currentIdx) return
        val old = currentIdx
        currentIdx = index
        adapter.setPlaying(index)
        val lm = binding.rvTracks.layoutManager as? LinearLayoutManager
        if (index in 0 until adapter.itemCount) {
            val first = lm?.findFirstVisibleItemPosition() ?: 0
            val last = lm?.findLastVisibleItemPosition() ?: 0
            if (index < first || index > last) {
                binding.rvTracks.post { binding.rvTracks.scrollToPosition(Math.max(0, index - 1)) }
            }
        }
        if (old >= 0) adapter.notifyItemChanged(old)
    }

    override fun onResume() {
        super.onResume()
        if (listener == null) {
            val c = PlayerManager.controller
            if (c != null) {
                listener = playerListener()
                try { c.addListener(listener!!) } catch (_: Exception) {}
            }
        }
        handler.post(ticker)
        refreshAll()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
        listener?.let { l -> try { PlayerManager.controller?.removeListener(l) } catch (_: Exception) {} }
        listener = null
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
