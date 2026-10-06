package com.example.micasaestucasa.ui.fragment.activities

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.micasaestucasa.R
import com.example.micasaestucasa.data.model.Casa
import com.example.micasaestucasa.databinding.FragmentOwnerActivitiesBinding
import com.example.micasaestucasa.ui.adapter.BookingAdapter
import com.example.micasaestucasa.ui.adapter.HouseAdapter
import com.example.micasaestucasa.ui.viewmodel.OwnerActivityViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch


class OwnerActivitiesFragment : Fragment(){

    private var _binding: FragmentOwnerActivitiesBinding? = null
    private val binding get() = _binding!!

    private val viewModel : OwnerActivityViewModel by viewModels()

    private lateinit var houseAdapter: HouseAdapter
    private lateinit var bookingAdapter: BookingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOwnerActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeUiState()
    }


    private fun observeUiState(){
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiState.collect{state ->
                    android.util.Log.d("DEBUG_OWNER", "Numero di case ricevute: ${state.housesList.size}")


                    //todo aggiungere progress bar in xml
                    //binding.progressBar.visibility = if(state.isLoading) View.VISIBLE else View.GONE

                    houseAdapter.submitList(state.housesList)
                    bookingAdapter.submitList(state.bookingList)

                    if(state.housesList.isEmpty() && !state.isLoading){
                        binding.tvEmptyHouses.visibility = View.VISIBLE
                        binding.rvMyHouses.visibility = View.GONE
                    }else{
                        binding.tvEmptyHouses.visibility = View.GONE
                        binding.rvMyHouses.visibility = View.VISIBLE
                    }

                    if(state.bookingList.isEmpty() && !state.isLoading){
                        binding.tvEmptyBookings.visibility = View.VISIBLE
                        binding.rvReceivedBookings.visibility = View.GONE
                    }else{
                        binding.tvEmptyBookings.visibility = View.GONE
                        binding.rvReceivedBookings.visibility = View.VISIBLE
                    }

                    if (state.isSuccess) {
                        Snackbar.make(binding.root, "Operazione completata!", Snackbar.LENGTH_SHORT).show()

                        viewModel.resetSuccess()
                    }

                    state.message?.let{
                        msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                        viewModel.clearMessage()
                    }

                    state.errorMessage?.let{
                        msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }

            }
        }
    }


    private fun showDeleteConfirmation(casa: Casa) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Elimina annuncio")
            .setMessage("Sei sicuro di voler eliminare '${casa.titolo}'? Questa azione non è reversibile.")
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Elimina") { _, _ ->
                viewModel.deleteHouse(casa.id)
            }
            .show()
    }


    private fun setupRecyclerView(){
        houseAdapter = HouseAdapter (
            isOwnerView = true,
            onCasaClick = { casa ->
                val bundle = Bundle().apply { putString("houseId", casa.id) }
               findNavController().navigate(R.id.action_ownerActivitiesFragment_to_detailedHouseFragment, bundle)
            },
            onEditClick = { casa ->
                val bundle = Bundle().apply { putString("houseId", casa.id) }
                findNavController().navigate(R.id.action_ownerActivitiesFragment_to_publishFragment, bundle)
            },
            onDeleteClick = { casa ->
                showDeleteConfirmation(casa)
            }
        )


        binding.rvMyHouses.apply{
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            adapter = houseAdapter
        }

        //TODO: verificare se funziona
        val snapHelper = androidx.recyclerview.widget.LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvMyHouses)

        bookingAdapter = BookingAdapter(
            isHostView = true,
            onAcceptClick = { uiModel ->
                viewModel.acceptBooking(uiModel.booking?.idBooking ?: "")
            },
            onRejectClick = {uiModel->
                viewModel.refuseBooking(uiModel.booking?.idBooking ?: "")
            },
            onItemClick = {
                val bundle = Bundle()
                bundle.putString("houseId", it.casa?.id)
                bundle.putString("bookingId", it.booking?.idBooking)
                bundle.putBoolean("isReadOnly", true)
                findNavController().navigate(R.id.action_ownerActivitiesFragment_to_bookingFragment, bundle)

            },
            onUserClick = {
                val bundle = Bundle()
                bundle.putString("userId", it)
                findNavController().navigate(R.id.action_ownerActivitiesFragment_to_userProfileFragment, bundle)

            })

        binding.rvReceivedBookings.apply{
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            adapter = bookingAdapter
        }

    }

    private fun setupListeners(){
        //pubblica
        binding.btnPublishHouse.setOnClickListener {
            findNavController().navigate(R.id.action_ownerActivitiesFragment_to_publishFragment)
        }
    }


    //TODO: altra soluzione: real time con addSnapshotListener nel repository
    override fun onStart() {
        super.onStart()
        viewModel.loadOwnerActivities()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }
}