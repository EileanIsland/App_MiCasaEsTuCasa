package com.example.micasaestucasa.ui.fragment

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.micasaestucasa.R
import com.example.micasaestucasa.databinding.FragmentHomeBinding
import com.example.micasaestucasa.ui.adapter.HouseAdapter
import com.example.micasaestucasa.ui.viewmodel.HomeViewModel
import com.google.android.material.color.MaterialColors

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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

        setupToolbar()
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
        houseAdapter = HouseAdapter { casa ->
            val bundle = Bundle().apply { putString("houseId", casa.id) }
            findNavController().navigate(R.id.action_homeFragment_to_detailedHouseFragment, bundle)
        }

        binding.rvHouses.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = houseAdapter
        }

    }


    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    //  Gestione ProgressBar
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

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
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text.toString()
                if (query.isNotEmpty()) {
                    navigateToSearch(query, isCategory = false)
                }
                true
            } else {
                false
            }
        }

        //bottone ricerca
        binding.btnExecuteSearch.setOnClickListener{
            val query = binding.etSearch.text.toString()
            navigateToSearch(query, isCategory = false)
        }

        //bottone pubblica
        binding.btnPublish.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_publishFragment)
        }

        //CATAEGORIE RICERCA
        binding.catCasa.setOnClickListener {
            navigateToSearch("CASA")
        }

        binding.catAppartamento.setOnClickListener {
            navigateToSearch("APPARTAMENTO")

        }

        binding.catStanza.setOnClickListener {
            navigateToSearch("STANZA")
        }

        binding.catGiardino.setOnClickListener {
            navigateToSearch("GIARDINO")
        }

        binding.btnVediTutto.setOnClickListener {
            navigateToSearch("ALL")
        }
    }

    private fun navigateToSearch(value: String, isCategory: Boolean = true) {
        val bundle = Bundle().apply {
            if (isCategory) {
                putString("category", value)
            } else {
                putString("query", value)
            }
        }
        findNavController().navigate(R.id.action_homeFragment_to_searchFragment, bundle)
    }

    private fun setupToolbar() {
        binding.homeToolbar.toolbarActions.removeAllViews()

        val notificationButton = ImageButton(requireContext()).apply {
            setImageResource(R.drawable.ic_notification)

            // Effetto click rotondo (ripple)
            val outValue = TypedValue()
            context.theme.resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless,
                outValue,
                true
            )
            setBackgroundResource(outValue.resourceId)

            val iconColor = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnSurface,
                android.graphics.Color.BLACK
            )
            setColorFilter(iconColor)

            contentDescription = getString(R.string.notifications_description)

            setOnClickListener {
                // TODO: Azione notifiche (es. snackbar o navigazione)
            }
        }

        binding.homeToolbar.toolbarActions.addView(notificationButton)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



