package com.example.micasaestucasa.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.micasaestucasa.R
import com.example.micasaestucasa.data.model.Casa
import com.example.micasaestucasa.databinding.FragmentSearchBinding
import com.example.micasaestucasa.ui.adapter.HouseAdapter
import com.example.micasaestucasa.ui.viewmodel.SearchViewModel
import com.example.micasaestucasa.utils.ViewUtils
import com.example.micasaestucasa.utils.ViewUtils.createChips
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import androidx.core.view.isEmpty

class SearchFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var houseAdapter: HouseAdapter
    private var googleMap: GoogleMap? = null
    private var lastDisplayedHouseIds: Set<String> = emptySet()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val initialQuery = arguments?.getString("query")
        val initialCategory = arguments?.getString("category")

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_search_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        if (initialQuery != null) viewModel.updateQuery(initialQuery)
        if (initialCategory != null) viewModel.updateCategory(initialCategory)

        setupMapTouchBehavior()
        setupRecyclerView()
        setupFilterListeners()
        setupVisibilityToggle()
        observeViewModel()
    }


    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading

                    //CHIP
                    if (binding.experienceChipGroup.isEmpty() && state.availableExperience.isNotEmpty()) {
                        binding.experienceChipGroup.createChips(state.availableExperience)
                    }

                    if (binding.servicesChipGroup.isEmpty() && state.availableService.isNotEmpty()) {
                        binding.servicesChipGroup.createChips(state.availableService)
                    }

                    // CATEGORIE
                    if (binding.typeFilter.adapter == null && state.availableCategory.isNotEmpty()) {
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            state.availableCategory
                        )
                        (binding.typeFilter as? AutoCompleteTextView)?.setAdapter(adapter)
                    }

                    // RISULTATI
                    houseAdapter.submitList(state.houses)
                    when{
                        state.houses.isNotEmpty() -> {
                            binding.resultsRecyclerView.visibility = View.VISIBLE
                            binding.noResultsText.visibility = View.GONE
                        }
                        !state.isLoading -> {
                            binding.resultsRecyclerView.visibility = View.GONE
                            binding.noResultsText.visibility = View.VISIBLE
                        }

                        else -> {
                            binding.resultsRecyclerView.visibility = View.GONE
                            binding.noResultsText.visibility = View.GONE
                        }
                    }

                    // Mappa
                    updateMapMarkers(state.houses)

                    // CONTATORI
                    binding.tvGuestsCount.text = state.numPerson.toString()
                    binding.tvRoomsCount.text = state.numBeds.toString()
                    binding.tvBathroomsCount.text = state.numBathroom.toString()

                    //SLIDER PREZZO
                    if (state.priceRange.size >= 2) {
                        val min = state.priceRange[0].toFloat()
                        val max = state.priceRange[1].toFloat()

                        binding.tvMinPrice.text = "${min.toInt()}€"
                        binding.tvMaxPrice.text = "${max.toInt()}€"

                        if (binding.priceSlider.values != listOf(min, max)) {
                            binding.priceSlider.setValues(min, max)
                        }
                    }

                    //GESTIONE ERRORI
                    state.errorMessage?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.errorShown()
                    }
                }
            }
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setupMapTouchBehavior() {
        // listener per i touch sul contenitore
        binding.mapOverlay.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN,
                     android.view.MotionEvent.ACTION_UP-> {
                    // Impedisce al contenitore scorrevole di intercettare il tocco
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    // Rilascia il controllo al contenitore
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            // Ritorniamo false per permettere all'evento di passare alla mappa sottostante
            false
        }
    }


    private fun setupRecyclerView() {
        houseAdapter = HouseAdapter { casa ->
            val bundle = Bundle().apply { putString("houseId", casa.id) }
            findNavController().navigate(R.id.action_searchFragment_to_detailedHouseFragment, bundle)
        }
        binding.resultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = houseAdapter
        }
    }


    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMapToolbarEnabled = false

        //listener sui marker
        map.setOnInfoWindowClickListener { marker ->
            val houseId = marker.tag as? String
            if(houseId != null){
                val bundle = Bundle().apply { putString("houseId", houseId) }
                findNavController().navigate(R.id.action_searchFragment_to_detailedHouseFragment, bundle)
            }
        }
        //TODO: DUBBIO SUI TAG DEI MARKER... come posso essere sicura che contengano proprio l'id della casa?
        // per come ho implementato la mappa in publishFragment contengono l'indirizzo...

        val currentHouses = viewModel.uiState.value.houses
        if(currentHouses.isNotEmpty()){
            updateMapMarkers(currentHouses)
        }else{
            val italy = LatLng(41.8719, 12.5674)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(italy, 5f))
        }

    }


    //filtro prezzo
    private fun setupVisibilityToggle() {
        binding.ivFiltersArrow.setOnClickListener {
            TransitionManager.beginDelayedTransition(binding.root as ViewGroup)
            val isCurrentlyGone = binding.advancedFiltersCard.isGone

            binding.advancedFiltersCard.isVisible = isCurrentlyGone
            binding.ivFiltersArrow.setImageResource(
                if (isCurrentlyGone) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }
    }


    private fun updateMapMarkers(houses: List<Casa>) {
        val map = googleMap ?: return

        val currentIds = houses.map { it.id }.toSet()
        if (currentIds == lastDisplayedHouseIds) return
        lastDisplayedHouseIds = currentIds

        map.clear()

        if (houses.isEmpty()) return

        val builder = LatLngBounds.Builder()
        var hasValidPoints = false

        houses.forEach { casa ->
            if(casa.latitudine != 0.0 && casa.longitudine!= 0.0) {
                val position = LatLng(casa.latitudine, casa.longitudine)

                val marker = map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(casa.titolo)
                        .snippet("${casa.prezzoNotte}€")
                )
                marker?.tag = casa.id
                builder.include(position)
                hasValidPoints = true
            }
        }

        if(hasValidPoints){
            try {
                val bounds = builder.build()
                if(houses.size == 1)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(houses[0].latitudine, houses[0].longitudine), 15f))
                else
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }

    }


    private fun setupFilterListeners() {
        // Query
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.updateQuery(text.toString())
        }

        // Selezione date
        binding.dateFilter.setOnClickListener {
            ViewUtils.showRangeDatePicker(childFragmentManager) { start, end, formattedDate ->
                viewModel.updateDates(listOf(start, end))
                binding.dateFilter.text = formattedDate
            }
        }

        // Guests
        binding.btnIncreaseGuests.setOnClickListener { viewModel.updateGuests(viewModel.uiState.value.numPerson + 1) }
        binding.btnDecreaseGuests.setOnClickListener {
            android.util.Log.d("SEARCH_DEBUG", "Click su decrease! Valore attuale: ${viewModel.uiState.value.numPerson}")
            val current = viewModel.uiState.value.numPerson
            if (current > 0) viewModel.updateGuests(current - 1)
        }

        //BEds
        binding.btnIncreaseBeds.setOnClickListener { viewModel.updateBeds(viewModel.uiState.value.numBeds + 1) }
        binding.btnDecreaseBeds.setOnClickListener {
            val current = viewModel.uiState.value.numBeds
            if (current > 0) viewModel.updateBeds(current - 1)
            //TODO sarebbe più sicuro decrementare nel viewmodel, fare i calcoli nel viewmodel RICORDARSI DI CAMBIARE
        }

        //bagni
        binding.btnIncreaseBathrooms.setOnClickListener { viewModel.updateBathroom(viewModel.uiState.value.numBathroom + 1) }
        binding.btnDecreaseBathrooms.setOnClickListener {
            val current = viewModel.uiState.value.numBathroom
            if (current > 0) viewModel.updateBathroom(current - 1)
        }

        // Listener Categoria Dropdown
        binding.typeFilter.setOnItemClickListener { _, _, position, _ ->
            val selected = binding.typeFilter.adapter.getItem(position).toString()
            viewModel.updateCategory(selected)
        }

        // Listener Chips Esperienza
        binding.experienceChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val selectedExperiences = checkedIds.map { id ->
                group.findViewById<Chip>(id).text.toString()
            }
            viewModel.updateExperiences(selectedExperiences)
        }

        //chip servizi
        binding.servicesChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val selectedServices = checkedIds.map { id ->
                group.findViewById<Chip>(id).text.toString()
            }
            viewModel.updateServices(selectedServices)
        }


        // Slider Prezzo
        // In setupFilterListeners()
        binding.priceSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            val min = values[0].toInt()
            val max = values[1].toInt()

            binding.tvMinPrice.text = "${min}€"
            binding.tvMaxPrice.text = "${max}€"

            viewModel.updatePriceRange(listOf(min, max))
        }

        //no filtri
        binding.resetFilters.setOnClickListener {
            viewModel.nofilter()
            binding.etSearch.setText("")
            binding.dateFilter.text = getString(R.string.select_dates)

            binding.servicesChipGroup.clearCheck()
            binding.experienceChipGroup.clearCheck()
            binding.priceSlider.setValues(0f, 1000f)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}




