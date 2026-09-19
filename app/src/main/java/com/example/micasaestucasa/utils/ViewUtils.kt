package com.example.micasaestucasa.utils

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.example.micasaestucasa.R
import com.example.micasaestucasa.data.model.Review
import com.example.micasaestucasa.ui.adapter.ReviewAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import androidx.core.util.Pair // IMPORTANTE: Deve essere questo Pair

object ViewUtils {

    /**
     * Popola chip group selezionabili con i dati specificati.
     */
    fun ChipGroup.createChips(
        items: List<String>
    ) {
        this.removeAllViews()
        if (items.isEmpty()) {
            this.visibility = View.GONE
            return
        }
        this.visibility = View.VISIBLE

        items.forEach { item ->
            val chip = Chip(this.context).apply{
                text = item
                isClickable = true
                isCheckable = true

            }
            this.addView(chip)
        }
    }

    /**
     * Crea chip solo visualizzazione
     * non selezionabili, non check
     */
    fun ChipGroup.createViewChips(
        items: List<String>
    ) {
        this.removeAllViews()
        if (items.isEmpty()) {
            this.visibility = View.GONE
            return
        }
        this.visibility = View.VISIBLE

        items.forEach { item ->
            val chip = Chip(this.context).apply{
                text = item
                isClickable = false
                isCheckable = false

            }
            this.addView(chip)
        }
    }

    /**
     * Popola un ChipGroup con i badge dell'utente aggiungendo icone specifiche.
     */
    fun ChipGroup.setupUserBadges(badgeList: List<String>) {
        this.removeAllViews()
        if (badgeList.isEmpty()) {
            this.visibility = View.GONE
            return
        }
        this.visibility = View.VISIBLE

        badgeList.forEach { badgeText ->
            val chip = Chip(this.context).apply {
                text = badgeText
                isClickable = false
                isCheckable = false

                val iconRes = when (badgeText.lowercase()) {
                    "super host" -> R.drawable.ic_super_host
                    "frequent traveller" -> R.drawable.ic_badge_traveller
                    "top rated" -> R.drawable.ic_star
                    "experienced host" -> R.drawable.ic_expert_host
                    else -> R.drawable.default_badge
                }

                setChipIconResource(iconRes)
                setChipIconTintResource(R.color.rose)
                isChipIconVisible = true
            }
            this.addView(chip)
        }
    }

    /**
     * Gestisce la visualizzazione delle recensioni (mostra 3 o tutte).
     */
    fun manageReviewsDisplay(
        reviews: List<Review>,
        isShowingAll: Boolean,
        adapter: ReviewAdapter,
        recyclerView: RecyclerView,
        noReviewsTextView: TextView,
        toggleButton: Button
    ) {
        val context = recyclerView.context
        if (reviews.isEmpty()) {
            recyclerView.visibility = View.GONE
            toggleButton.visibility = View.GONE
            noReviewsTextView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            toggleButton.visibility = if (reviews.size > 3) View.VISIBLE else View.GONE
            noReviewsTextView.visibility = View.GONE

            val listToShow = if (isShowingAll) reviews else reviews.take(3)
            adapter.submitList(listToShow)

            toggleButton.text = if (isShowingAll) {
                context.getString(R.string.showLessReviews)
            } else {
                context.getString(R.string.show_all_reviews)
            }
        }
    }

    /**
     * Mostra un selettore di intervallo date (Check-in / Check-out)
     * Restituisce i dati come Long (timestamp).
     */
    fun showRangeDatePicker(
        fragmentManager: FragmentManager,
        onDateSelected: (Long, Long, String) -> Unit
    ) {
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())

        val today = MaterialDatePicker.todayInUtcMilliseconds()

        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Seleziona le date")
            .setCalendarConstraints(constraintsBuilder.build())
            .setSelection(Pair(today, today))
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second

            if (startDate != null && endDate != null) {
                onDateSelected(startDate, endDate, dateRangePicker.headerText)
            }
        }

        dateRangePicker.show(fragmentManager, "DATE_RANGE_PICKER")
    }
}