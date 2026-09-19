package com.example.micasaestucasa.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.micasaestucasa.databinding.ItemEditPhotoBinding

class EditPhotoAdapter(
    private val onDeleteClick: (Int) -> Unit
) : ListAdapter<String, EditPhotoAdapter.PhotoViewHolder>(DiffCallback) {

    inner class PhotoViewHolder(
        private val binding: ItemEditPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: String) {
            Glide.with(binding.photoImage.context)
                .load(photo)
                .centerCrop()
                .into(binding.photoImage)

            binding.deletePhotoButton.setOnClickListener {
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onDeleteClick(currentPos)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemEditPhotoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    }
}



