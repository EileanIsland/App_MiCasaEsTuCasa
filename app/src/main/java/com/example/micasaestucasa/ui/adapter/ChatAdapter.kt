package com.example.micasaestucasa.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.micasaestucasa.R
import com.example.micasaestucasa.data.model.ChatPreview
import com.example.micasaestucasa.databinding.ItemChatBinding

class ChatAdapter(
    private val onChatClick: (ChatPreview) -> Unit
) : ListAdapter<ChatPreview, ChatAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = getItem(position)
        holder.bind(chat, onChatClick)
    }

    class ChatViewHolder(private val binding: ItemChatBinding):
        RecyclerView.ViewHolder(binding.root){

        fun bind(chat: ChatPreview, onChatClick: (ChatPreview) -> Unit){
            binding.tvName.text = chat.otherUserName
            binding.tvLastMessage.text = chat.lastMessage

            Glide.with(binding.root.context)
                .load(chat.otherUserPhoto)
                .placeholder(R.drawable.ic_person).circleCrop()
                .error(R.drawable.ic_person)
                .into(binding.ivProfile)


            binding.root.setOnClickListener{onChatClick(chat)}

        }

    }


    class ChatDiffCallback: DiffUtil.ItemCallback<ChatPreview>(){
        override fun areItemsTheSame(oldItem: ChatPreview, newItem: ChatPreview): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatPreview, newItem: ChatPreview): Boolean {
            return oldItem == newItem
        }

    }

}