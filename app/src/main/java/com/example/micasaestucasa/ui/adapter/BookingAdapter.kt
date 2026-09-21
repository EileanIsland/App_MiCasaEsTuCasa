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
import com.example.micasaestucasa.data.model.User
import com.example.micasaestucasa.databinding.ItemBookingBinding

/**
 * Adapter riutilizzabile che accetta un [BookingUi] per mostrare
 * i dati congiunti di Prenotazione, Casa e Utente controparte.
 */
class BookingAdapter(
    private val isHostView: Boolean,
    private val onModifyClick: ((BookingUi) -> Unit)? = null,
    private val onAcceptClick: ((BookingUi) -> Unit)? = null,
    private val onRejectClick: ((BookingUi) -> Unit)? = null,
    private val onItemClick: ((BookingUi) -> Unit)? = null
) : ListAdapter<BookingUi, BookingAdapter.BookingViewHolder>(BookingUiCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookingViewHolder(binding, isHostView, onModifyClick, onAcceptClick, onRejectClick, onItemClick)
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
        private val onItemClick: ((BookingUi) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uiModel: BookingUi) {
            val context = itemView.context
            val booking = uiModel.booking
            val casa = uiModel.casa
            val user = if(isHostView) uiModel.guest!! else uiModel.owner!!


            binding.tvHouseName.text = casa?.titolo ?: context.getString(R.string.segnaposto)
            binding.tvLocation.text = casa?.citta ?: "Posizione non specificata"

            binding.tvGuests.text = "${casa?.ospitiMassimi ?: 0} ospiti"
            binding.tvStatus.text = booking?.stato // Assicurati che 'stato' esista in Booking.kt

            val dataInizioStr = booking?.dataInizio
            val dataFineStr = booking?.dataFine
            binding.tvDates.text = "$dataInizioStr - $dataFineStr"

            val imageUrl = casa?.immagini?.firstOrNull()
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .centerCrop()
                .into(binding.ivHouseCover)

            // 4. DATI UTENTE CONTROPARTE (item_user.xml incluso via ViewBinding)
            binding.layoutUser.visibility = View.VISIBLE
            binding.userInfo.apply {
                tvUserName.text = (user.name + user.surname) ?: "Nome non specificato"
                //TODO: Altre info utente
            }

            if (isHostView) {
                binding.btnModify.visibility = View.GONE

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

                binding.layoutActions.visibility = View.VISIBLE
                binding.btnModify.visibility = View.VISIBLE
            }

            binding.btnModify.setOnClickListener { onModifyClick?.invoke(uiModel) }
            binding.btnAccept.setOnClickListener { onAcceptClick?.invoke(uiModel) }
            binding.btnReject.setOnClickListener { onRejectClick?.invoke(uiModel) }

            itemView.setOnClickListener {
                onItemClick?.invoke(uiModel)
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









