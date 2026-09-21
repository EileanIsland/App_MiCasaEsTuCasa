package com.example.micasaestucasa.ui.fragment.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.micasaestucasa.databinding.FragmentGuestActivitiesBinding
import com.example.micasaestucasa.ui.adapter.BookingAdapter
import com.example.micasaestucasa.ui.viewmodel.GuestActivityViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class GuestActivitiesFragment : Fragment() {

    // Utilizziamo viewModels() poiché questo ViewModel è specifico per questa schermata
    private val viewModel: GuestActivityViewModel by viewModels()

    private var _binding: FragmentGuestActivitiesBinding? = null
    private val binding get() = _binding!!

    private lateinit var bookingAdapter: BookingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuestActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeUiState()
    }

    private fun setupRecyclerView() {
        // Inizializziamo l'adapter specificando che NON siamo nella vista Host
        bookingAdapter = BookingAdapter(
            isHostView = false,
            onModifyClick = { uiModel ->
                // Azione al click su "Modifica" (Adatta con la tua azione di navigazione reale)
                Toast.makeText(
                    requireContext(),
                    "Modifica viaggio: ${uiModel.booking?.idBooking}",
                    Toast.LENGTH_SHORT
                ).show()

                // Esempio Navigazione:
                // val action = GuestActivitiesFragmentDirections.actionToModifyBooking(uiModel.booking.idBooking)
                // findNavController().navigate(action)
            },
            onItemClick = { uiModel ->
                // Azione al click sull'intera card per vedere i dettagli
                Toast.makeText(
                    requireContext(),
                    "Dettagli viaggio: ${uiModel.booking?.idBooking}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    // 1. Gestione indicatore di caricamento (se presente nel tuo XML)
                    // binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // 2. Aggiornamento della lista nell'adapter
                    bookingAdapter.submitList(state.bookingList)

                    // 3. Gestione dello stato vuoto (tvEmpty)
                    if (state.bookingList.isEmpty() && !state.isLoading) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvBookings.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvBookings.visibility = View.VISIBLE
                    }

                    // 4. Gestione centralizzata dei messaggi di errore tramite Snackbar
                    state.errorMessage?.let { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}












//TODO AGGIUNGERE SWIPE
/*binding.swipeRefresh.setOnRefreshListener {
    viewModel.loadAllActivities()
}*/