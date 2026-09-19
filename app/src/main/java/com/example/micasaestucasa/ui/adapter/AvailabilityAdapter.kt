package com.example.micasaestucasa.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.micasaestucasa.data.model.Disponibilita
import com.example.micasaestucasa.databinding.ItemAvailabilityBinding
import java.text.SimpleDateFormat
import java.util.Locale

// Nota: Aggiunto DiffCallback nel costruttore del ListAdapter
class AvailabilityAdapter(
    private val onDeleteClick: (Int) -> Unit
) : ListAdapter<Disponibilita, AvailabilityAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemAvailabilityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun bind(item: Disponibilita) {
            val startDate = dateFormat.format(item.inizio)
            val endDate = dateFormat.format(item.fine)
            binding.availabilityText.text = "$startDate - $endDate"

            binding.deleteAvailabilityButton.setOnClickListener {
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onDeleteClick(currentPos)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAvailabilityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Disponibilita>() {
        override fun areItemsTheSame(oldItem: Disponibilita, newItem: Disponibilita): Boolean {
            return oldItem.inizio == newItem.inizio && oldItem.fine == newItem.fine
        }

        override fun areContentsTheSame(oldItem: Disponibilita, newItem: Disponibilita): Boolean {
            return oldItem == newItem
        }
    }
}
