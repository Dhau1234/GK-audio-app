package com.hindigk.audio.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Color
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = getItem(position)
        val ctx = holder.b.root.context
        holder.b.tvIndex.text = (position + 1).toString()
        holder.b.tvTrackName.text = t.name
        val playing = position == currentPlaying
        holder.b.ivPlaying.visibility = if (playing) View.VISIBLE else View.GONE
        holder.b.tvIndex.setBackgroundResource(
            if (playing) R.drawable.bg_index_playing else R.drawable.bg_index
        )
        holder.b.tvTrackName.setTextColor(
            if (playing) ctx.getColor(R.color.brand) else ctx.getColor(R.color.text_main)
        )
        holder.b.tvIndex.setTextColor(
            if (playing) Color.WHITE else ctx.getColor(R.color.text_sub)
        )
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
