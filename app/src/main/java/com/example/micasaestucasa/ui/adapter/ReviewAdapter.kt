package com.example.micasaestucasa.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.micasaestucasa.R
import com.example.micasaestucasa.data.model.Review
import com.example.micasaestucasa.databinding.ItemReviewBinding
import com.example.micasaestucasa.utils.DateUtils.formatDate

class ReviewAdapter : ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(DiffCallback) {

    class ReviewViewHolder(
        private val binding: ItemReviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            binding.reviewerName.text = review.reviewerName
            binding.reviewRating.text = "${review.rating}/5"
            binding.reviewDate.text = formatDate(review.date)
            binding.reviewText.text = review.testo

            if (review.reviewerImageUrl.isNotEmpty()) {
                Glide.with(binding.reviewerImage.context)
                    .load(review.reviewerImageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(binding.reviewerImage)
            } else {
                binding.reviewerImage.setImageResource(R.drawable.ic_person)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem.reviewerName == newItem.reviewerName && oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem == newItem
        }
    }
}