package com.example.micasaestucasa.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.micasaestucasa.R
import com.example.micasaestucasa.data.model.Casa
import com.example.micasaestucasa.databinding.ItemHouseBinding

class HouseAdapter(
    private val onCasaClick: (Casa) -> Unit
) : ListAdapter<Casa, HouseAdapter.HomeViewHolder>(DiffCallback) {

    class HomeViewHolder(private val binding: ItemHouseBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(casa: Casa, onCasaClick: (Casa) -> Unit) {
            binding.apply {
                tvTitle.text = casa.titolo
                tvPrice.text = "${casa.prezzoNotte} €/notte"
                tvCity.text = casa.citta
                tvGuests.text = casa.ospitiMassimi.toString()
                tvRooms.text = casa.numeroCamere.toString()
                tvRating.text = casa.valutazioneMedia.toString()

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
        holder.bind(getItem(position), onCasaClick)
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



