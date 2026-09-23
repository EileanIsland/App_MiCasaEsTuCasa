package com.example.micasaestucasa.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.micasaestucasa.R
import com.example.micasaestucasa.data.model.Disponibilita
import com.example.micasaestucasa.databinding.FragmentPublishBinding
import com.example.micasaestucasa.ui.adapter.AvailabilityAdapter
import com.example.micasaestucasa.ui.adapter.EditPhotoAdapter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.chip.Chip
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.micasaestucasa.ui.viewmodel.PublishViewModel
import com.example.micasaestucasa.utils.ViewUtils.createChips
import kotlinx.coroutines.launch
import com.example.micasaestucasa.utils.ViewUtils.showRangeDatePicker
import com.google.android.material.chip.ChipGroup
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import android.location.Address
import android.widget.ArrayAdapter
import kotlin.coroutines.resume


//TODO: IN activities:
//val id =
 //   arguments?.getString("houseId")
//CasaRepository.getCasaById(id)


class PublishFragment : Fragment(), OnMapReadyCallback{
    private val viewModel: PublishViewModel by viewModels()
    private var _binding: FragmentPublishBinding? = null
    private val binding get() = _binding!!

    //Flags per evitare di rigenerare DropDown e chip
    private var isCategoriesInitialized = false
    private var isTagsInitialized = false

    private lateinit var photoAdapter: EditPhotoAdapter
    private lateinit var availabilityAdapter : AvailabilityAdapter

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri -> viewModel.addImage(uri.toString()) }
    }

    private lateinit var googleMap: GoogleMap


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPublishBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val houseId = arguments?.getString("houseId")
        if(houseId != null){
            viewModel.loadCasa(houseId)
        }

        setupRecyclerViews()
        setupMap()
        setupInputListeners()
        setupActionListeners()
        observeUiState()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    //gestione stato errore/caricamento
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.publishButton.isEnabled = state.isFormValid && !state.isLoading

                    if(state.isSuccess){
                        Toast.makeText(requireContext(), "Annuncio Pubblicato!", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                        return@collect
                    }

                    state.errorMessage?.let{ msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                        viewModel.clearError()
                    }

                    // popolamento campi
                    if (binding.titleEditText.text.isNullOrEmpty() && state.titolo.isNotEmpty()) binding.titleEditText.setText(state.titolo)
                    if (binding.descriptionEditText.text.isNullOrEmpty() && state.descrizione.isNotEmpty()) binding.descriptionEditText.setText(state.descrizione)
                    if (binding.cityEditText.text.isNullOrEmpty() && state.citta.isNotEmpty()) binding.cityEditText.setText(state.citta)
                    if (binding.addressEditText.text.isNullOrEmpty() && state.indirizzo.isNotEmpty()) binding.addressEditText.setText(state.indirizzo)
                    if (binding.priceEditText.text.isNullOrEmpty() && state.prezzoNotte > 0) binding.priceEditText.setText(state.prezzoNotte.toString())

                    //dropDown tipologia casa
                    if(!isCategoriesInitialized && state.availableCategory.isNotEmpty()){
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            state.availableCategory
                        )

                        binding.typeDropdown.setAdapter(adapter)

                        if(state.tipologia.isNotEmpty())
                            binding.typeDropdown.setText(state.tipologia, false)

                        isCategoriesInitialized = true
                    }

                    //liste foto e disponibilità
                    photoAdapter.submitList(state.images)
                    availabilityAdapter.submitList(state.disponibilita)

                    //TODO: non la centra sulla città così
                    //la mappa diventa visibile solo dopo aver inserito una città
                    if(state.citta.isNotBlank()){
                        binding.mapContainer.visibility = View.VISIBLE
                        if(state.latitudine != 0.0 && state.longitudine != 0.0)
                            updateMap(state.latitudine, state.longitudine)
                    }else{
                        binding.mapContainer.visibility = View.GONE
                    }

                    //chip e tag
                    if (!isTagsInitialized && state.availableService.isNotEmpty()) {
                        binding.servicesChipGroup.createChips(state.availableService)
                        binding.experienceChipGroup.createChips(state.availableExperience)
                        binding.rulesChipGroup.createChips(state.availableRule)
                        setupChipClickListeners()
                        isTagsInitialized = true
                    }

                    //CHIP
                    updateChips(binding.servicesChipGroup, state.servizi)
                    updateChips(binding.experienceChipGroup, state.esperienza)
                    updateChips(binding.rulesChipGroup, state.regole)
                }
            }
        }
    }


    private fun setupInputListeners() {
        binding.titleEditText.doAfterTextChanged { viewModel.updateTitolo(it.toString()) }
        binding.descriptionEditText.doAfterTextChanged { viewModel.updateDescrizione(it.toString()) }

        binding.cityEditText.doAfterTextChanged { text ->
            val cittaInserita = text.toString().trim()
            viewModel.updateCitta(cittaInserita)

            if (cittaInserita.isNotBlank()) {
                binding.mapContainer.visibility = View.VISIBLE

                viewLifecycleOwner.lifecycleScope.launch {
                    centraMappa(cittaInserita)
                }
            }else{
                binding.mapContainer.visibility = View.GONE
            }
        }

        binding.addressEditText.doAfterTextChanged { text ->
            val indirizzo = text.toString().trim()
            viewModel.updateIndirizzo(indirizzo)

            if (indirizzo.isNotBlank() && ::googleMap.isInitialized) {
                val cittaAttuale = viewModel.uiState.value.citta

                viewLifecycleOwner.lifecycleScope.launch {
                    centraMappa(cittaAttuale, indirizzo)
                }
            }
        }

        binding.priceEditText.doAfterTextChanged {
            viewModel.updatePrezzo(it.toString().toDoubleOrNull() ?: 0.0)
        }

        binding.typeDropdown.setOnItemClickListener { parent, _, position, _ ->
            val selectedType = parent.getItemAtPosition(position) as String
            viewModel.updateTipologia(selectedType)
        }
    }

    private suspend fun centraMappa(citta: String, indirizzo: String = ""){
        if(citta.isBlank()) return

        val stringaRicerca = if(indirizzo.isNotBlank()) "$citta, $indirizzo" else citta
        val geocoder = Geocoder(requireContext())

        try{
            val posizione = getAddressAsync(geocoder, stringaRicerca)

            if(posizione != null){
                val coordinate = LatLng(posizione.latitude, posizione.longitude)

                val livelloZoom = if (indirizzo.isNotBlank()) 16f else 13f

                withContext(Dispatchers.Main){
                    googleMap.clear()
                    googleMap.addMarker(MarkerOptions().position(coordinate).title(stringaRicerca))
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(coordinate, livelloZoom))

                    viewModel.updateLatitudine(coordinate.latitude)
                    viewModel.updateLongitudine(coordinate.longitude)

                }
            }
        }catch(e: Exception){
            e.printStackTrace()
        }

    }

    private suspend fun getAddressAsync(geocoder: Geocoder, stringaRicerca: String): Address?{
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            suspendCancellableCoroutine {
                continuation ->
                geocoder.getFromLocationName(stringaRicerca, 1, object: Geocoder.GeocodeListener{
                    override fun onGeocode(addresses: MutableList<Address>){
                        continuation.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?){
                        continuation.resume(null)
                    }
                })
            }
        }else{
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(stringaRicerca, 1)?.firstOrNull()
            }
        }
    }


    fun setupActionListeners(){
        binding.addPhotoButton.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.addAvailabilityButton.setOnClickListener {
            showRangeDatePicker(childFragmentManager) { start, end, _ ->
                viewModel.addDisponibilita(Disponibilita(start, end))
            }
        }

        binding.publishButton.setOnClickListener {
            viewModel.save()
            navToDetailedHouse(viewModel.uiState.value.houseId)
        }
    }


    private fun setupRecyclerViews() {
        //ListAdapter: listener
        photoAdapter = EditPhotoAdapter { position ->
            val currentImages = viewModel.uiState.value.images
            if (position in currentImages.indices) {
                viewModel.removeImage(currentImages[position])
            }
        }
        binding.photosRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.photosRecyclerView.adapter = photoAdapter

        availabilityAdapter = AvailabilityAdapter { position ->
            val currentAvailabilities = viewModel.uiState.value.disponibilita
            if (position in currentAvailabilities.indices) {
                viewModel.removeDisponibilita(currentAvailabilities[position])
            }
        }
        binding.availabilityRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.availabilityRecyclerView.adapter = availabilityAdapter
    }


    private fun setupChipClickListeners() {
        val setupListenerForGroup = { group: ChipGroup, onAdd: (String) -> Unit, onRemove: (String) -> Unit ->
            for (i in 0 until group.childCount) {
                val chip = group.getChildAt(i) as? Chip
                chip?.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) onAdd(chip.text.toString()) else onRemove(chip.text.toString())
                }
            }
        }

        setupListenerForGroup(binding.servicesChipGroup, viewModel::addServizio, viewModel::removeServizio)
        setupListenerForGroup(binding.experienceChipGroup, viewModel::addEsperienza, viewModel::removeEsperienza)
        setupListenerForGroup(binding.rulesChipGroup, viewModel::addRegola, viewModel::removeRegola)
    }


    private fun updateChips(group: ChipGroup, selectedValues: List<String>) {
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? Chip
            chip?.let {

                val shouldBeChecked = selectedValues.contains(it.text.toString())
                if (it.isChecked != shouldBeChecked) {
                    // Rimuove temporaneamente il listener per evitare loop infiniti durante l'aggiornamento programmatico
                    it.setOnCheckedChangeListener(null)
                    it.isChecked = shouldBeChecked
                    // Riassegna il listener corretto in base al gruppo
                    it.setOnCheckedChangeListener { _, isChecked ->
                        when (group.id) {
                            R.id.servicesChipGroup -> if (isChecked) viewModel.addServizio(it.text.toString()) else viewModel.removeServizio(it.text.toString())
                            R.id.experienceChipGroup -> if (isChecked) viewModel.addEsperienza(it.text.toString()) else viewModel.removeEsperienza(it.text.toString())
                            R.id.rulesChipGroup -> if (isChecked) viewModel.addRegola(it.text.toString()) else viewModel.removeRegola(it.text.toString())
                        }
                    }
                }
            }
        }
    }


    //MAPPA
    private fun setupMap(){
        val mapFragment = childFragmentManager.findFragmentById(
            R.id.mapContainer
        ) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }


    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val state = viewModel.uiState.value

        if (state.latitudine != 0.0 && state.longitudine != 0.0) {
            val position = LatLng(state.latitudine, state.longitudine)
            map.clear()
            map.addMarker(MarkerOptions().position(position).title("Posizione salvata"))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
        }

        map.setOnMapClickListener { latLng ->
            map.clear()
            map.addMarker(MarkerOptions().position(latLng).title("Posizione selezionata"))
            viewModel.updateLatitudine(latLng.latitude)
            viewModel.updateLongitudine(latLng.longitude)
        }
    }


    private fun updateMap(lat: Double, lng: Double){
        if (!::googleMap.isInitialized) return
        val position = LatLng(lat, lng)
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(position).title("Casa"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 12f))
    }


    private fun navToDetailedHouse(id:String){
        val bundle = Bundle()
        bundle.putString("houseId", id)

        findNavController().navigate(
            R.id.action_publishFragment_to_detailedHouseFragment,
            bundle
        )
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }

}


