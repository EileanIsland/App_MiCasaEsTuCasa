package com.example.micasaestucasa.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.micasaestucasa.R
import com.example.micasaestucasa.data.model.Casa
import com.example.micasaestucasa.databinding.ItemHouseBinding


//TODO: modifica adapter per poterlo usare sia per ite_house per guest che per owner

/**
 * Adapter flessibile per la visualizzazione delle case.
 *
 * @param isOwnerView Indica se la visualizzazione è per un proprietario (true) o per un ospite (false).
 * @param onCasaClick Callback per gestire il click su una casa.
 * @param onEditClick Callback per gestire il click su un pulsante di modifica.
 * @param onDeleteClick Callback per gestire il click su un pulsante di cancellazione.
 */

class HouseAdapter(
    private val isOwnerView: Boolean,
    private val onCasaClick: (Casa) -> Unit,
    private val onEditClick: ((Casa) -> Unit)? = null,
    private val onDeleteClick: ((Casa) -> Unit)? = null
) : ListAdapter<Casa, HouseAdapter.HomeViewHolder>(DiffCallback) {

    class HomeViewHolder(private val binding: ItemHouseBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            casa: Casa,
            isOwnerView: Boolean,
            onCasaClick: (Casa) -> Unit,
            onEditClick: ((Casa) -> Unit)?,
            onDeleteClick: ((Casa) -> Unit)?
        ) {

            binding.apply {
                tvTitle.text = casa.titolo
                tvPrice.text = "${casa.prezzoNotte} €/notte"
                tvCity.text = casa.citta
                tvGuests.text = casa.ospitiMassimi.toString()
                tvRooms.text = casa.numeroCamere.toString()
                tvRating.text = casa.valutazioneMedia.toString()

                //bottoni proprietario
                layoutActions.isVisible = isOwnerView

                if (casa.immagini.isNotEmpty()) {
                    Glide.with(ivHouseCover.context)
                        .load(casa.immagini.first())
                        .centerCrop()
                        .placeholder(R.drawable.placeholder)
                        .into(ivHouseCover)
                } else {
                    Glide.with(ivHouseCover.context).clear(ivHouseCover)
                }

                root.setOnClickListener { onCasaClick(casa) }
                btnEdit.setOnClickListener { onEditClick?.invoke(casa) }
                btnDelete.setOnClickListener { onDeleteClick?.invoke(casa) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val binding = ItemHouseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HomeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        holder.bind(getItem(position), isOwnerView, onCasaClick, onEditClick, onDeleteClick)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Casa>() {
        override fun areItemsTheSame(oldItem: Casa, newItem: Casa): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Casa, newItem: Casa): Boolean {
            return oldItem == newItem
        }
    }
}



