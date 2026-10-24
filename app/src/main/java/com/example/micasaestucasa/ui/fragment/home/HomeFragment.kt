package com.example.micasaestucasa.ui.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.micasaestucasa.R
import com.example.micasaestucasa.databinding.FragmentHomeBinding
import com.example.micasaestucasa.ui.adapter.HouseAdapter
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var houseAdapter: HouseAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearchBar()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }


    private fun setupSearchBar(){
        binding.etSearch.text?.clear()
        binding.etSearch.clearFocus()
    }
    private fun setupRecyclerView() {
        houseAdapter = HouseAdapter(
            isOwnerView = false,
            onCasaClick = { casa ->
                val bundle = Bundle().apply { putString("houseId", casa.id) }
                findNavController().navigate(
                    R.id.action_homeFragment_to_detailedHouseFragment,
                    bundle
                )
            }

        )

        binding.rvHouses.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = houseAdapter
        }

    }


    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    //  Gestione ProgressBar
                    binding.topProgressBar.isVisible = state.isLoading
                    binding.homeScroll.alpha = if (state.isLoading) 0.6f else 1.0f
                    binding.etSearch.isEnabled = !state.isLoading
                    binding.btnExecuteSearch.isEnabled = !state.isLoading

                    when {
                        state.houses.isNotEmpty() -> {
                            houseAdapter.submitList(state.houses)
                            binding.rvHouses.visibility = View.VISIBLE
                            binding.tvNoResults.visibility = View.GONE
                        }
                        !state.isLoading -> {
                            binding.rvHouses.visibility = View.GONE
                            binding.tvNoResults.visibility = View.VISIBLE
                        }
                        else -> {
                            binding.rvHouses.visibility = View.GONE
                            binding.tvNoResults.visibility = View.GONE
                        }
                    }

                    // Gestione Errori
                    state.errorMessage?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.errorShown()
                    }
                }
            }
        }
    }



    private fun setupClickListeners() {
        //bottone pubblica casa
        binding.btnPublish.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_publishFragment)
        }

        //card per la ricerca
        binding.searchCard.setOnClickListener {
            binding.etSearch.requestFocus()
        }

        //input text ricerca
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text.toString()
                if (query.isNotEmpty()) {
                    navigateToSearch(query)
                }
                true
            } else {
                false
            }
        }

        //bottone ricerca
        binding.btnExecuteSearch.setOnClickListener{
            val query = binding.etSearch.text.toString()
            navigateToSearch(query)
        }

        //bottone pubblica
        binding.btnPublish.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_publishFragment)
        }

        //CATAEGORIE RICERCA
        binding.catCasa.setOnClickListener {
            navigateToSearch("Casa")
        }

        binding.catAppartamento.setOnClickListener {
            navigateToSearch("Appartamento")

        }

        binding.catStanza.setOnClickListener {
            navigateToSearch("stanza")
        }

        binding.catGiardino.setOnClickListener {
            navigateToSearch("giardino")
        }

        binding.btnVediTutto.setOnClickListener {
            navigateToSearch("ALL")
        }
    }

    private fun navigateToSearch(value: String) {
        if(value == "ALL"){
            val bundle = Bundle().apply {
                putString("query", "")
            }
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment, bundle)
            return
        }

        val bundle = Bundle().apply {
            putString("query", value)
        }
        findNavController().navigate(R.id.action_homeFragment_to_searchFragment, bundle)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}