package com.example.micasaestucasa.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import com.example.micasaestucasa.R
import com.example.micasaestucasa.ui.models.ReportedUser
import com.example.micasaestucasa.databinding.ItemReportedUserBinding

class ReportedUserAdapter(
    private val onSuspend: (ReportedUser) -> Unit,
    private val onBlock: (ReportedUser) -> Unit,
    private val onDismiss: (ReportedUser) -> Unit
) : ListAdapter<ReportedUser, ReportedUserAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemReportedUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReportedUser) {
            binding.userItem.tvUserName.text = "${item.user.name} ${item.user.surname}"
            binding.userItem.ivUserPhoto.visibility = View.GONE

            binding.chipCount.text = "${item.reports.size} Segnalazioni"

            val reportAdapter = ReportAdapter()
            binding.rvReports.apply {
                layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
                adapter = reportAdapter
                isNestedScrollingEnabled = false
            }
            reportAdapter.submitList(item.reports)

            binding.rvReports.isVisible = item.isExpanded
            val icon = if (item.isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            binding.titoloSegnalazioni.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)

            binding.titoloSegnalazioni.setOnClickListener {
                item.isExpanded = !item.isExpanded

                androidx.transition.TransitionManager.beginDelayedTransition(binding.root as ViewGroup)

                notifyItemChanged(bindingAdapterPosition)
            }

            binding.btnSuspend.setOnClickListener { onSuspend(item) }
            binding.btnBlock.setOnClickListener { onBlock(item) }
            binding.btnDismiss.setOnClickListener { onDismiss(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemReportedUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<ReportedUser>() {
        override fun areItemsTheSame(oldItem: ReportedUser, newItem: ReportedUser) = oldItem.user.id == newItem.user.id
        override fun areContentsTheSame(oldItem: ReportedUser, newItem: ReportedUser) = oldItem == newItem
    }
}