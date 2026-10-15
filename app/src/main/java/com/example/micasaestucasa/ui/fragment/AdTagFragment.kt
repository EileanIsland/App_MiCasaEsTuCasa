package com.example.micasaestucasa.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import com.example.micasaestucasa.R
import com.example.micasaestucasa.databinding.DialogAddTagBinding
import com.example.micasaestucasa.databinding.FragmentAdTagBinding
import com.example.micasaestucasa.ui.viewmodel.AdTagViewMod
import com.example.micasaestucasa.ui.viewmodel.AdTagUiState
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AdTagFragment : Fragment() {
    private var _binding: FragmentAdTagBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdTagViewMod by viewModels()
    private var isFabOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdTagBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFabMenu()
        setupCard()
        observeUiState()
    }

    private fun setupFabMenu() {
        binding.fabMain.setOnClickListener {
            toggleFabMenu()
        }

        binding.fabAddTag.setOnClickListener {
            showAddTagDialog()
            toggleFabMenu()
        }

        binding.fabDeleteTag.setOnClickListener {
            val state = viewModel.uiState.value
            if (!state.isSelectionMode) {
                viewModel.toggleSelectionMode()
                Snackbar.make(binding.root, "Seleziona i tag da eliminare", Snackbar.LENGTH_SHORT).show()
            } else {
                if (state.selectedTags.isNotEmpty()) {
                    eliminaDialog(state.selectedTags.size)
                } else {
                    viewModel.toggleSelectionMode()
                }
            }
            toggleFabMenu()
        }

        binding.fabShadowOverlay.setOnClickListener {
            if (isFabOpen) toggleFabMenu()
        }
    }

    private fun toggleFabMenu() {
        isFabOpen = !isFabOpen

        val rotation = if (isFabOpen) 45f else 0f
        binding.fabMain.animate().rotation(rotation).setDuration(300).start()

        if (isFabOpen) {
            binding.fabShadowOverlay.isVisible = true
            showFabChild(binding.fabAddTag, 0)
            showFabChild(binding.fabDeleteTag, 50)
        } else {
            hideFabChild(binding.fabAddTag)
            hideFabChild(binding.fabDeleteTag)
            binding.fabShadowOverlay.isVisible = false
        }
    }

    private fun showFabChild(view: View, delay: Long) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.translationY = 100f
        view.animate().alpha(1f).translationY(0f).setDuration(300).setStartDelay(delay).start()
    }

    private fun hideFabChild(view: View) {
        view.animate().alpha(0f).translationY(100f).setDuration(300).setStartDelay(0).withEndAction {
            view.visibility = View.INVISIBLE
        }.start()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                populateGroup(binding.cgServices, state.services, "servizi", state)
                populateGroup(binding.cgFeatures, state.features, "esperienza", state)
                populateGroup(binding.cgRules, state.rules, "regole", state)

                // UI pulsante elimina in base alla modalità
                if (state.isSelectionMode) {
                    binding.fabDeleteTag.setImageResource(R.drawable.ic_single_check)
                    binding.fabDeleteTag.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.green)
                } else {
                    binding.fabDeleteTag.setImageResource(R.drawable.ic_minus)
                    binding.fabDeleteTag.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.error_Red)
                }

                // Gestione feedback (SnackBar) - Usiamo takeIf per evitare stringhe vuote
                state.userFeedback?.takeIf { it.isNotBlank() }?.let { message ->
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    viewModel.resetFeedback()
                }

                state.errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_Red))
                        .show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun populateGroup(group: ChipGroup, tags: List<String>, category: String, state: AdTagUiState) {
        group.removeAllViews()
        tags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag
                isCheckable = state.isSelectionMode
                isChecked = state.selectedTags.contains(category to tag)

                // In modalità selezione, il click aggiunge/rimuove dalla lista eliminazione
                setOnClickListener {
                    if (state.isSelectionMode) {
                        viewModel.toggleSelection(category to tag)
                    }
                }
            }
            group.addView(chip)
        }
    }

    private fun setupCard() {
        binding.tvServicesHeader.setOnClickListener {
            toggleVisibility(binding.cgServices, binding.tvServicesHeader)
        }
        binding.tvFeaturesHeader.setOnClickListener {
            toggleVisibility(binding.cgFeatures, binding.tvFeaturesHeader)
        }
        binding.tvRulesHeader.setOnClickListener {
            toggleVisibility(binding.cgRules, binding.tvRulesHeader)
        }
    }

    private fun toggleVisibility(view: View, header: TextView) {
        val isVisible = view.isVisible
        TransitionManager.beginDelayedTransition(binding.root as ViewGroup)
        view.isVisible = !isVisible
        val icon = if (isVisible) R.drawable.ic_expand_more else R.drawable.ic_expand_less
        header.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)
    }

    private fun showAddTagDialog() {
        val dialogBinding = DialogAddTagBinding.inflate(layoutInflater)

        val displayCategories = arrayOf("Servizi", "Esperienza", "Regole")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, displayCategories)
        dialogBinding.spinnerCategory.adapter = adapter

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Aggiungi nuova Keyword")
            .setView(dialogBinding.root)
            .setPositiveButton("Aggiungi") { _, _ ->
                val name = dialogBinding.etTagName.text.toString()
                val selectedPos = dialogBinding.spinnerCategory.selectedItemPosition
                val categoryForDb = displayCategories[selectedPos]

                if (name.isNotBlank()) {
                    viewModel.saveTag(categoryForDb, name)
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun eliminaDialog(count: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Elimina Keyword")
            .setMessage("Sei sicuro di voler eliminare $count keyword selezionate?")
            .setPositiveButton("Elimina") { _, _ ->
                viewModel.deleteSelectedTags()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

