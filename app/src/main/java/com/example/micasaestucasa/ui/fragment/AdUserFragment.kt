package com.example.micasaestucasa.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.compose.ui.semantics.text
import com.example.micasaestucasa.R
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.micasaestucasa.databinding.FragmentAdUserBinding
import com.example.micasaestucasa.ui.adapter.ReportedUserAdapter
import com.example.micasaestucasa.ui.viewmodel.AdUsersViewMod
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AdUserFragment : Fragment(R.layout.fragment_ad_user) {

    private var _binding: FragmentAdUserBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdUsersViewMod by viewModels()

    private lateinit var adapter: ReportedUserAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdUserBinding.bind(view)

        setupRecyclerView()
        observeUiState()
    }

    private fun setupRecyclerView() {
        adapter = ReportedUserAdapter(
            onSuspend = { reportedUser ->
                showSuspendDialog(reportedUser.user.id, reportedUser.user.name)
            },
            onBlock = { reportedUser ->
                showBlockConfirmationDialog(reportedUser.user.id, reportedUser.user.name)
            }
        )
        binding.rvReportedUsers.adapter = adapter
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Gestione Caricamento
                binding.progressBar.isVisible = state.isLoading

                // Gestione Lista e Badge
                adapter.submitList(state.reportedUsers)
                binding.countUsers.text = state.count.toString()
                binding.countUsers.isVisible = state.count > 0

                // Stato Vuoto
                binding.tvEmptyState.isVisible = !state.isLoading && state.reportedUsers.isEmpty()

                // Messaggi di Feedback
                state.userFeedback?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                    viewModel.clearUserFeedback()
                }

                // Messaggi di Errore
                state.errorMessage?.takeIf { it.isNotBlank() }?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(resources.getColor(R.color.error_Red, null))
                        .show()
                    viewModel.clearErrorMessage()
                }
            }
        }
    }

    /**
     * Dialog per selezionare la durata della sospensione
     */
    private fun showSuspendDialog(userId: String, userName: String) {
        val durations = arrayOf("1 Giorno", "3 Giorni", "7 Giorni", "30 Giorni")
        val daysValues = intArrayOf(1, 3, 7, 30)
        var selectedDays = daysValues[0]

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sospendi $userName")
            .setSingleChoiceItems(durations, 0) { _, which ->
                selectedDays = daysValues[which]
            }
            .setPositiveButton("Sospendi") { _, _ ->
                viewModel.suspendUser(userId, selectedDays)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    /**
     * Dialog di conferma per il blocco definitivo
     */
    private fun showBlockConfirmationDialog(userId: String, userName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Blocca Definitivamente")
            .setMessage("Sei sicuro di voler bloccare $userName? L'utente non potrà più accedere all'app.")
            .setPositiveButton("Blocca") { _, _ ->
                viewModel.blockUser(userId)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}