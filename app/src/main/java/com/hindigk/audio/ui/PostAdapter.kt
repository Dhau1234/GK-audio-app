package com.hindigk.audio.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hindigk.audio.data.BlogPost
import com.hindigk.audio.data.Utils
import com.hindigk.audio.databinding.ItemPostBinding

class PostAdapter(
    private val onClick: (BlogPost) -> Unit
) : ListAdapter<BlogPost, PostAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = getItem(position)
        holder.b.tvPostTitle.text = p.title
        val date = Utils.formatDate(p.published)
        holder.b.tvPostMeta.text =
            if (date.isBlank()) p.metaLine() else "${p.metaLine()}  •  $date"
    }

    inner class VH(val b: ItemPostBinding) : RecyclerView.ViewHolder(b.root) {
        init {
            b.root.setOnClickListener { onClick(getItem(bindingAdapterPosition)) }
        }
    }

    object Diff : DiffUtil.ItemCallback<BlogPost>() {
        override fun areItemsTheSame(oldItem: BlogPost, newItem: BlogPost) =
            oldItem.title == newItem.title && oldItem.published == newItem.published

        override fun areContentsTheSame(oldItem: BlogPost, newItem: BlogPost) =
            oldItem == newItem
    }
}
