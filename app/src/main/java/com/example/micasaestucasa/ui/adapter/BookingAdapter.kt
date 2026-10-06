package com.example.micasaestucasa.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.micasaestucasa.R
import com.example.micasaestucasa.data.model.BookingUi
import com.example.micasaestucasa.databinding.ItemBookingBinding
import com.example.micasaestucasa.utils.DateUtils.formatRange

/**
 * Adapter riutilizzabile che accetta un [BookingUi] per mostrare
 * i dati congiunti di Prenotazione, Casa e Utente controparte.
 */
class BookingAdapter(
    private val isHostView: Boolean,
    private val onModifyClick: ((BookingUi) -> Unit)? = null,
    private val onAcceptClick: ((BookingUi) -> Unit)? = null,
    private val onRejectClick: ((BookingUi) -> Unit)? = null,
    private val onReviewClick: ((BookingUi) -> Unit)? = null,
    private val onItemClick: ((BookingUi) -> Unit)? = null,
    private val onUserClick: ((String) -> Unit)? = null
) : ListAdapter<BookingUi, BookingAdapter.BookingViewHolder>(BookingUiCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookingViewHolder(binding, isHostView, onModifyClick, onAcceptClick, onRejectClick, onReviewClick, onItemClick, onUserClick)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BookingViewHolder(
        private val binding: ItemBookingBinding,
        private val isHostView: Boolean,
        private val onModifyClick: ((BookingUi) -> Unit)?,
        private val onAcceptClick: ((BookingUi) -> Unit)?,
        private val onRejectClick: ((BookingUi) -> Unit)?,
        private val onReviewClick: ((BookingUi) -> Unit)?,
        private val onItemClick: ((BookingUi) -> Unit)?,
        private val onUserClick: ((String) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uiModel: BookingUi) {
            val context = itemView.context
            val booking = uiModel.booking
            val casa = uiModel.casa
            val user = if(isHostView) uiModel.guest!! else uiModel.owner!!
            val ruolo = if(isHostView) "Ospite" else "Proprietario"


            binding.tvHouseName.text = casa?.titolo ?: context.getString(R.string.segnaposto)
            binding.tvLocation.text = casa?.citta ?: "Posizione non specificata"

            binding.tvGuests.text = "${booking?.numeroOspiti ?: 0} ospiti"
            binding.tvStatus.text = booking?.stato


            binding.tvDates.text = formatRange( booking?.dataInizio, booking?.dataFine)

            val imageUrl = casa?.immagini?.firstOrNull()
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .centerCrop()
                .into(binding.ivHouseCover)

            binding.layoutUser.visibility = View.VISIBLE
            binding.userInfo.apply {
                tvUserName.text = (user.name + " " + user.surname)
                tvUserRole.text = ruolo
                tvUserRole.visibility = View.VISIBLE
            }

            if (isHostView) {
                binding.btnModify.visibility = View.GONE
                binding.btnReview.visibility = View.GONE


                if (booking?.stato == "In attesa" || booking?.stato == "Pending") {
                    binding.layoutActions.visibility = View.VISIBLE
                    binding.btnAccept.visibility = View.VISIBLE
                    binding.btnReject.visibility = View.VISIBLE
                } else {
                    binding.layoutActions.visibility = View.GONE
                }
            } else {
                binding.btnAccept.visibility = View.GONE
                binding.btnReject.visibility = View.GONE

                // si può modificare la prenotazione solo se è in attesa o pending
                val canModify = booking?.stato == "In attesa" || booking?.stato == "Pending"

                if (canModify) {
                    binding.layoutActions.visibility = View.VISIBLE
                    binding.btnModify.visibility = View.VISIBLE
                    binding.btnReview.visibility = View.GONE

                } else {
                    binding.btnReview.visibility = View.VISIBLE
                    binding.btnModify.visibility = View.GONE
                }
            }

            binding.btnModify.setOnClickListener { onModifyClick?.invoke(uiModel) }
            binding.btnAccept.setOnClickListener { onAcceptClick?.invoke(uiModel) }
            binding.btnReject.setOnClickListener { onRejectClick?.invoke(uiModel) }
            binding.btnReview.setOnClickListener { onReviewClick?.invoke(uiModel) }

            itemView.setOnClickListener {
                onItemClick?.invoke(uiModel)
            }

            binding.userInfo.root.setOnClickListener {
                user?.id?.let { id -> onUserClick?.invoke(id) }
            }
        }
    }

    private object BookingUiCallback : DiffUtil.ItemCallback<BookingUi>() {
        override fun areItemsTheSame(oldItem: BookingUi, newItem: BookingUi): Boolean {
            return oldItem.booking?.idBooking == newItem.booking?.idBooking
        }

        override fun areContentsTheSame(oldItem: BookingUi, newItem: BookingUi): Boolean {
            return oldItem == newItem
        }
    }
}









