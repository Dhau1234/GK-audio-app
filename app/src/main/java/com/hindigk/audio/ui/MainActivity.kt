package com.hindigk.audio.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.hindigk.audio.R
import com.hindigk.audio.audio.PlayerManager
import com.hindigk.audio.data.Blogs
import com.hindigk.audio.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val blogs = Blogs.all

    private val fragments = arrayOfNulls<CategoryFragment>(blogs.size)
    private var currentBlogPos = 0

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { safe { updateMini() } }
        override fun onPlaybackStateChanged(playbackState: Int) { safe { updateMini() } }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { safe { updateMini() } }
        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) { safe { updateMini() } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermission()

        if (savedInstanceState != null) {
            currentBlogPos = savedInstanceState.getInt("blog")
            for (i in blogs.indices) {
                fragments[i] = supportFragmentManager.findFragmentByTag("blog_$i") as? CategoryFragment
            }
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val pos = when (item.itemId) {
                R.id.nav_lucent -> 0
                R.id.nav_manjar -> 1
                R.id.nav_misc -> 2
                R.id.nav_darpan -> 3
                R.id.nav_saar -> 4
                else -> 0
            }
            if (pos != currentBlogPos) showBlog(pos)
            true
        }

        binding.miniPlay.setOnClickListener { PlayerManager.togglePlayPause() }
        binding.miniNext.setOnClickListener { PlayerManager.next() }
        binding.miniCard.setOnClickListener { openFullPlayer() }

        if (savedInstanceState == null) showBlog(0)
        else {
            binding.bottomNav.selectedItemId = when (currentBlogPos) {
                1 -> R.id.nav_manjar
                2 -> R.id.nav_misc
                3 -> R.id.nav_darpan
                4 -> R.id.nav_saar
                else -> R.id.nav_lucent
            }
        }

        PlayerManager.attach(this)
        PlayerManager.connect(this) { c ->
            c.addListener(playerListener)
            runOnUiThread { try { updateMini() } catch (_: Exception) {} }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("blog", currentBlogPos)
    }

    private fun showBlog(pos: Int) {
        try {
            val fm = supportFragmentManager
            val tx = fm.beginTransaction()
            for (i in blogs.indices) {
                val f = fm.findFragmentByTag("blog_$i")
                if (f != null && i != pos) tx.hide(f)
            }
            val tag = "blog_$pos"
            val existing = fm.findFragmentByTag(tag)
            val f: CategoryFragment
            if (existing == null) {
                f = CategoryFragment.newInstance(pos)
                tx.add(R.id.fragmentContainer, f, tag)
            } else {
                f = existing as CategoryFragment
                tx.show(f)
            }
            fragments[pos] = f
            tx.commitAllowingStateLoss()
            currentBlogPos = pos
        } catch (_: Exception) {
        }
    }

    private fun openFullPlayer() {
        val post = PlayerManager.currentPost ?: return
        try {
            val i = Intent(this, PlayerActivity::class.java)
            i.putExtra(PlayerActivity.EXTRA_POST, post)
            startActivity(i)
        } catch (_: Exception) {
        }
    }

    fun updateMini() {
        val c = PlayerManager.controller
        if (c == null) {
            binding.miniCard.visibility = android.view.View.GONE
            return
        }
        val has = c.mediaItemCount > 0 && c.currentMediaItem != null
        binding.miniCard.visibility = if (has) android.view.View.VISIBLE else android.view.View.GONE
        if (!has) return
        binding.miniTitle.text = c.mediaMetadata.title ?: ""
        binding.miniPlay.setImageResource(
            if (c.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        )
    }

    private fun safe(block: () -> Unit) {
        runOnUiThread { try { block() } catch (_: Exception) {} }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }

    override fun onDestroy() {
        try { PlayerManager.controller?.removeListener(playerListener) } catch (_: Exception) {}
        super.onDestroy()
    }
}
