package com.example.micasaestucasa.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.micasaestucasa.data.model.Message
import com.example.micasaestucasa.data.model.Booking
import com.example.micasaestucasa.databinding.*
import com.example.micasaestucasa.utils.DateUtils.formatDate

class MessageAdapter(
    private val currentUid: String,
    private val onImageClick: (String) -> Unit,
    private val onContextClick: (String, String) -> Unit,
    private val onReviewHouse: ((String) -> Unit)?,
    private val onReviewUser: (String) -> Unit
): ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    private var bookingsMap: Map<String, Booking> = emptyMap()

    private val VIEW_TYPE_SENT_TEXT = 1
    private val VIEW_TYPE_RECEIVED_TEXT = 2
    private val VIEW_TYPE_SENT_IMAGE = 3
    private val VIEW_TYPE_RECEIVED_IMAGE = 4
    private val VIEW_TYPE_CONTEXT = 5
    private val VIEW_TYPE_BOOKING_SUMMARY = 6

    fun updateBookings(newBookings: Map<String, Booking>) {
        this.bookingsMap = newBookings
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        val isMe = message.senderId == currentUid

        return when {
            message.type == "booking_summary" -> VIEW_TYPE_BOOKING_SUMMARY
            message.type == "context" -> VIEW_TYPE_CONTEXT
            message.type == "image" -> if (isMe) VIEW_TYPE_SENT_IMAGE else VIEW_TYPE_RECEIVED_IMAGE
            else -> if (isMe) VIEW_TYPE_SENT_TEXT else VIEW_TYPE_RECEIVED_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT_TEXT -> SentViewHolder(ItemMessageSentBinding.inflate(inflater, parent, false))
            VIEW_TYPE_RECEIVED_TEXT -> ReceivedViewHolder(ItemMessageReceivedBinding.inflate(inflater, parent, false))
            VIEW_TYPE_SENT_IMAGE -> SentImageViewHolder(ItemMessageSentBinding.inflate(inflater, parent, false))
            VIEW_TYPE_RECEIVED_IMAGE -> ReceivedImageViewHolder(ItemMessageReceivedBinding.inflate(inflater, parent, false))
            VIEW_TYPE_CONTEXT -> ContextViewHolder(ItemChatContextBinding.inflate(inflater, parent, false))
            VIEW_TYPE_BOOKING_SUMMARY -> BookingSummaryViewHolder(ItemBookingSummaryBinding.inflate(inflater, parent, false))
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
            is ContextViewHolder -> holder.bind(message)
            is BookingSummaryViewHolder -> holder.bind(message)
        }
    }

    inner class BookingSummaryViewHolder(private val binding: ItemBookingSummaryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvContextTitle.text = message.text
            binding.tvContextSubtitle.text = "Riepilogo prenotazione"

            Glide.with(binding.ivContextHouse.context)
                .load(message.url)
                .centerCrop()
                .into(binding.ivContextHouse)

            val bookingId = message.contextId
            val booking = bookingsMap[bookingId]

            val isHost = onReviewHouse == null

            if (booking != null) {
                val canReviewHouse = booking.canReviewHouse(isHost)
                val canReviewUser = booking.canReviewUser()

                binding.layoutReviewActions.visibility = if (canReviewHouse || canReviewUser) View.VISIBLE else View.GONE

                if (canReviewHouse) {
                    binding.btnReviewHouse.visibility = View.VISIBLE
                    binding.btnReviewHouse.setOnClickListener {
                        bookingId?.let { id -> onReviewHouse?.invoke(id) }
                    }
                } else {
                    binding.btnReviewHouse.visibility = View.GONE
                }

                if (canReviewUser) {
                    binding.btnReviewUser.visibility = View.VISIBLE
                    binding.btnReviewUser.text = if (isHost) "Recensisci Ospite" else "Recensisci Host"
                    binding.btnReviewUser.setOnClickListener {
                        bookingId?.let { id -> onReviewUser(id) }
                    }
                } else {
                    binding.btnReviewUser.visibility = View.GONE
                }

            } else {
                binding.layoutReviewActions.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                if (bookingId != null) onContextClick(bookingId, "BOOKING")
            }
        }
    }

    inner class ContextViewHolder(private val binding: ItemChatContextBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvContextTitle.text = message.text
            binding.tvContextSubtitle.text = when(message.contextType) {
                "HOUSE" -> "In merito a questa casa"
                "BOOKING" -> "Dettagli della prenotazione"
                else -> "Contesto conversazione"
            }

            Glide.with(binding.ivContextHouse.context)
                .load(message.url)
                .centerCrop()
                .into(binding.ivContextHouse)

            binding.root.setOnClickListener {
                val contextId = message.contextId
                val contextType = message.contextType
                if (contextId != null && contextType != null) {
                    onContextClick(contextId, contextType)
                }
            }
        }
    }

    inner class SentViewHolder(private val binding: ItemMessageSentBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvMessage.text = message.text
            binding.tvTime.text = formatDate(message.timestamp)
        }
    }

    inner class ReceivedViewHolder(private val binding: ItemMessageReceivedBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvMessage.text = message.text
            binding.tvTime.text = formatDate(message.timestamp)
        }
    }

    inner class SentImageViewHolder(private val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvTime.text = formatDate(message.timestamp)
            binding.ivMessageImage.visibility = View.VISIBLE
            Glide.with(binding.ivMessageImage.context).load(message.url).centerCrop().into(binding.ivMessageImage)
            binding.ivMessageImage.setOnClickListener { message.url?.let { onImageClick(it) } }
        }
    }

    inner class ReceivedImageViewHolder(private val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvTime.text = formatDate(message.timestamp)
            binding.ivMessageImage.visibility = View.VISIBLE
            Glide.with(binding.ivMessageImage.context).load(message.url).centerCrop().into(binding.ivMessageImage)
            binding.ivMessageImage.setOnClickListener { message.url?.let { onImageClick(it) } }
        }
    }


    class MessageDiffCallback: DiffUtil.ItemCallback<Message>(){
        override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
    }


}



