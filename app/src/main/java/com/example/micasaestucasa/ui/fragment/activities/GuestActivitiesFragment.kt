package com.example.micasaestucasa.ui.fragment.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.micasaestucasa.databinding.FragmentGuestActivitiesBinding
import com.example.micasaestucasa.ui.adapter.BookingAdapter
import com.example.micasaestucasa.ui.viewmodel.GuestActivityViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import com.example.micasaestucasa.R


class GuestActivitiesFragment : Fragment() {

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

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("refresh_list")
            ?.observe(viewLifecycleOwner) { refresh ->
                if (refresh) {
                    viewModel.loadGuestActivities()
                    findNavController().currentBackStackEntry?.savedStateHandle?.remove<Boolean>("refresh_list")
                }
            }

        setupRecyclerView()
        observeUiState()
    }

    private fun setupRecyclerView() {
        bookingAdapter = BookingAdapter(
            isHostView = false,
            onModifyClick = { uiModel ->
                val bundle = Bundle().apply {
                    putString("bookingId", uiModel.booking?.idBooking)
                    putString("houseId", uiModel.casa?.id)
                    putBoolean("isReadOnly", false)
                }
                findNavController().navigate(R.id.action_guestActivitiesFragment_to_bookingFragment, bundle)

                            },
            onReviewClick = { uiModel ->

                val bundle = Bundle().apply {
                    putString("bookingId", uiModel.booking?.idBooking)
                    putString("houseId", uiModel.casa?.id)

                }
                findNavController().navigate(R.id.action_guestActivitiesFragment_to_reviewFragment, bundle)

            },
            onItemClick = { uiModel ->
                val bundle = Bundle().apply {
                    putString("bookingId", uiModel.booking?.idBooking)
                    putString("houseId", uiModel.casa?.id)
                    putBoolean("isReadOnly", true)
                }
                findNavController().navigate(R.id.action_guestActivitiesFragment_to_bookingFragment, bundle)
            },
            onUserClick = { userId ->
                val bundle = Bundle().apply {
                    android.util.Log.d("NAV_TEST", "Cliccato su utente: $userId")
                    putString("userId", userId)
                }
                findNavController().navigate(R.id.action_guestActivitiesFragment_to_userProfileFragment, bundle)
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

                    // TODO: Gestione progress bar (non mi ricordo se l'ho messa nel fragment xml controllare)
                    // binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    bookingAdapter.submitList(state.bookingList)

                    if (state.bookingList.isEmpty() && !state.isLoading) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvBookings.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvBookings.visibility = View.VISIBLE
                    }

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