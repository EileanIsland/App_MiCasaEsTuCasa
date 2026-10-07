package com.example.micasaestucasa.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.micasaestucasa.data.model.Message
import com.example.micasaestucasa.databinding.ItemMessageReceivedBinding
import com.example.micasaestucasa.databinding.ItemMessageSentBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val currentUid: String,
    private val onImageClick: (String) -> Unit
    ): ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    private val VIEW_TYPE_SENT_TEXT = 1
    private val VIEW_TYPE_RECEIVED_TEXT = 2
    private val VIEW_TYPE_SENT_IMAGE = 3
    private val VIEW_TYPE_RECEIVED_IMAGE = 4

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        val isMe = message.senderId == currentUid

        return if(message.type == "image"){
            if(isMe) VIEW_TYPE_SENT_IMAGE else VIEW_TYPE_RECEIVED_IMAGE
        }else{
            if(isMe) VIEW_TYPE_SENT_TEXT else VIEW_TYPE_RECEIVED_TEXT

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT_TEXT -> SentViewHolder(ItemMessageSentBinding.inflate(inflater, parent, false))
            VIEW_TYPE_RECEIVED_TEXT -> ReceivedViewHolder(ItemMessageReceivedBinding.inflate(inflater, parent, false))
            VIEW_TYPE_SENT_IMAGE -> SentImageViewHolder(ItemMessageSentBinding.inflate(inflater, parent, false))
            VIEW_TYPE_RECEIVED_IMAGE -> ReceivedImageViewHolder(ItemMessageReceivedBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentViewHolder -> holder.bind(message)
            is ReceivedViewHolder -> holder.bind(message)
            is SentImageViewHolder -> holder.bind(message)
            is ReceivedImageViewHolder -> holder.bind(message)
        }
    }


    inner class SentViewHolder(
        private val binding: ItemMessageSentBinding
    ): RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvMessage.text = message.text
            binding.tvTime.text = formatTime(message.timestamp)
        }
    }

    inner class ReceivedViewHolder(
        private val binding: ItemMessageReceivedBinding
    ): RecyclerView.ViewHolder(binding.root){
        fun bind(message: Message){
            binding.tvMessage.text = message.text
            binding.tvTime.text = formatTime(message.timestamp)
        }
    }

    inner class SentImageViewHolder(private val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvTime.text = formatTime(message.timestamp)
            binding.ivMessageImage.visibility = View.VISIBLE
            Glide.with(binding.ivMessageImage.context)
                .load(message.url)
                .centerCrop()
                .into(binding.ivMessageImage)

            binding.ivMessageImage.setOnClickListener {
                message.url?.let { url -> onImageClick(url) }
            }
        }
    }

    inner class ReceivedImageViewHolder(private val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvTime.text = formatTime(message.timestamp)
            binding.ivMessageImage.visibility = View.VISIBLE

            Glide.with(binding.ivMessageImage.context)
                .load(message.url)
                .centerCrop()
                .into(binding.ivMessageImage)

            binding.ivMessageImage.setOnClickListener {
                message.url?.let { url -> onImageClick(url) }
            }
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = Date(timestamp)
        return sdf.format(date)
    }

    class MessageDiffCallback: DiffUtil.ItemCallback<Message>(){
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }

    }




}


