package com.hindigk.audio.ui

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hindigk.audio.R
import com.hindigk.audio.data.Track
import com.hindigk.audio.databinding.ItemTrackBinding

class TrackAdapter(
    private val onClick: (Track, Int) -> Unit
) : ListAdapter<Track, TrackAdapter.VH>(Diff) {

    private var currentPlaying = -1
    private var bgNormal: Drawable? = null
    private var bgPlaying: Drawable? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = getItem(position)
        holder.b.tvIndex.text = (position + 1).toString()
        holder.b.tvTrackName.text = t.name
        val playing = position == currentPlaying
        holder.b.ivPlaying.visibility = if (playing) View.VISIBLE else View.GONE
        if (bgPlaying == null) {
            bgPlaying = ContextCompat.getDrawable(holder.b.root.context, R.drawable.bg_index_playing)
        }
        if (bgNormal == null) {
            bgNormal = ContextCompat.getDrawable(holder.b.root.context, R.drawable.bg_index)
        }
        holder.b.tvIndex.background = if (playing) bgPlaying else bgNormal
        if (playing) {
            holder.b.tvTrackName.setTextColor(ContextCompat.getColor(holder.b.root.context, R.color.brand))
            holder.b.tvIndex.setTextColor(android.graphics.Color.WHITE)
        } else {
            holder.b.tvTrackName.setTextColor(ContextCompat.getColor(holder.b.root.context, R.color.text_main))
            holder.b.tvIndex.setTextColor(ContextCompat.getColor(holder.b.root.context, R.color.text_sub))
        }
    }

    fun setPlaying(index: Int) {
        val old = currentPlaying
        currentPlaying = index
        if (old >= 0) notifyItemChanged(old)
        if (index >= 0) notifyItemChanged(index)
    }

    inner class VH(val b: ItemTrackBinding) : RecyclerView.ViewHolder(b.root) {
        init {
            b.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos >= 0) onClick(getItem(pos), pos)
            }
        }
    }

    object Diff : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track) = oldItem.url == newItem.url
        override fun areContentsTheSame(oldItem: Track, newItem: Track) = oldItem == newItem
    }
}
