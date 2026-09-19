package com.example.micasaestucasa.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.micasaestucasa.databinding.ItemFullscreenPhotoBinding

class FullScreenPhotoAdapter : ListAdapter<String, FullScreenPhotoAdapter.FullScreenPhotoHolder>(DiffCallback) {

    class FullScreenPhotoHolder(
        private val binding: ItemFullscreenPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(imageUrl: String) {
            Glide.with(binding.root.context)
                .load(imageUrl)
                .into(binding.fullscreenImage)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FullScreenPhotoHolder {
        val binding = ItemFullscreenPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FullScreenPhotoHolder(binding)
    }

    override fun onBindViewHolder(
        holder: FullScreenPhotoHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}


