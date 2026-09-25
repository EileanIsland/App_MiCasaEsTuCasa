package com.example.micasaestucasa.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.micasaestucasa.R
import com.example.micasaestucasa.data.model.Casa
import com.example.micasaestucasa.databinding.FragmentDetailedHouseBinding
import com.example.micasaestucasa.ui.adapter.PhotoAdapter
import com.example.micasaestucasa.ui.adapter.ReviewAdapter
import com.example.micasaestucasa.ui.viewmodel.DetailedHouseViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.bumptech.glide.Glide

import com.example.micasaestucasa.utils.ViewUtils.createViewChips
import com.example.micasaestucasa.utils.ViewUtils.manageReviewsDisplay

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import kotlinx.coroutines.launch

class DetailedHouseFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentDetailedHouseBinding? = null
    private val binding get() = _binding!!

    private val viewModel : DetailedHouseViewModel by viewModels()
    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var photoAdapter: PhotoAdapter

    private var googleMap: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailedHouseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupButtons()
        setupMapFragment()
        observeViewModel()

        val houseId = arguments?.getString("houseId")
        if (houseId != null) {
            viewModel.loadHouseById(houseId)
        }else{
            Toast.makeText(
                requireContext(),
                "Errore nel caricamento della casa",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    android.util.Log.d("DEBUG_HOUSE", "Casa presente nello stato: ${state.house != null}")

                    // caricamento
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // Casa
                    state.house?.let { house ->
                        displayHouse(house)

                        updateMapLocation(house)
                        photoAdapter.submitList(house.immagini)

                    }

                    if (state.isHouseNotFound) {
                        Toast.makeText(requireContext(), "Casa non trovata", Toast.LENGTH_LONG).show()
                        findNavController().popBackStack()
                    }

                    // Proprietario
                    state.owner?.let { owner ->
                        binding.ownerInfo.tvUserName.text = getString(R.string.nome_cognome_user, owner.name, owner.surname)
                        binding.ownerInfo.tvUserRole.text = getString(R.string.proprietario)
                        Glide.with(this@DetailedHouseFragment)
                            .load(owner.profileImageUrl)
                            .placeholder(R.drawable.ic_person)
                            .circleCrop()
                            .into(binding.ownerInfo.ivUserPhoto)
                    }

                    // Statistiche del proprietario
                    state.ownerStat.let { stats ->

                        binding.ownerInfo.tvUserRating.visibility = View.VISIBLE
                        binding.ownerInfo.tvUserStats.visibility = View.VISIBLE
                        binding.ownerInfo.tvUserRole.visibility = View.VISIBLE


                        binding.ownerInfo.tvUserRating.text = getString(
                            R.string.rating_num_rec,
                            stats.ratingMedia,
                            stats.numeroRecensioni
                        )

                        binding.ownerInfo.tvUserStats.text= getString(
                            R.string.num_soggiorni_annunci,
                            stats.numeroSoggiorni,
                            stats.numeroAnnunci
                        )
                    }

                    // Recensioni
                    manageReviewsDisplay(
                        state.houseReviews,
                        state.isShowingAllReviews,
                        reviewAdapter,
                        binding.reviewsRecyclerView,
                        binding.tvNoReviews,
                        binding.showAllReviewsButton
                    )

                    // Bottoni visibilità in base all'utente
                    binding.bookButton.visibility = if (state.isOwner) View.GONE else View.VISIBLE
                    binding.contactOwnerButton.visibility = if (state.isOwner) View.GONE else View.VISIBLE


                    // Messaggi di errore
                    state.errorMessage?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.errorShown()
                    }
                }
            }
        }
    }


    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMapToolbarEnabled = false

        viewModel.uiState.value.house?.let {
            updateMapLocation(it)
        }
    }

    private fun setupMapFragment(){
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_detailed) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

    }


    private fun setupAdapters(){
        //FOTO
        photoAdapter = PhotoAdapter{ position ->
            val images = viewModel.uiState.value.house?.immagini?: emptyList()
            PhotoGalleryDialogFragment.newInstance(
                images,
                position
            ).show(
                childFragmentManager,
                "photoGallery"
            )
        }
        binding.photosRecyclerView.apply{
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = photoAdapter
        }

        //RECENSIONI
        reviewAdapter = ReviewAdapter()
        binding.reviewsRecyclerView.apply{
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewAdapter
            isNestedScrollingEnabled = false
        }

    }


    private fun updateMapLocation(house: Casa) {
        val map = googleMap ?: return
        val position = LatLng(house.latitudine, house.longitudine)

        map.clear()
        map.addMarker(MarkerOptions().position(position).title(house.titolo))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
    }


    private fun displayHouse(house: Casa) {
        binding.houseTitle.text = house.titolo
        binding.houseLocation.text = house.citta
        binding.housePrice.text = getString(R.string.detailed_house_prezzo_notte, house.prezzoNotte)
        binding.houseTypeText.text = getString(R.string.detailed_house_tipo, house.tipo.label)
        binding.houseGuestsText.text = getString(R.string._detailed_house_ospiti_massimi, house.ospitiMassimi)
        binding.houseRoomsText.text = getString(R.string.detailed_house_camereNumero, house.numeroCamere)
        binding.houseBathroomsText.text = getString(R.string.detailed_house_bagni, house.numeroBagni)
        binding.houseDescription.text = house.descrizione
        binding.houseRating.text = getString(R.string.rating_num_rec, viewModel.uiState.value.houseRating, viewModel.uiState.value.numRec)

        binding.experienceTagsGroup.createViewChips(house.esperienza)
        binding.servicesTagsGroup.createViewChips(house.servizi)
        binding.rulesTagsGroup.createViewChips(house.regole)
    }


    private fun setupButtons(){
        binding.contactOwnerButton.setOnClickListener {
            //TODO apertura chat proprietario
            Toast.makeText(
                requireContext(),
                "Apri chat proprietario",
                Toast.LENGTH_SHORT
            ).show()

        }

        binding.bookButton.setOnClickListener {
            val bundle = Bundle().apply{putString("houseId", viewModel.uiState.value.house?.id)}
            findNavController()
                .navigate(R.id.action_detailedHouseFragment_to_bookHouseFragment, bundle)

            Toast.makeText(
                requireContext(),
                getString(R.string.apri_prenotazione),
                Toast.LENGTH_SHORT
            ).show()

        }

        //bottone mostra tutte le recensioni
        binding.showAllReviewsButton.setOnClickListener {
            viewModel.toggleReviews()
        }

        //CARD PROPRIETARIO
        binding.ownerInfo.root.setOnClickListener {
            val state = viewModel.uiState.value

            if (state.isOwner) {
                findNavController().navigate(R.id.action_detailedHouseFragment_to_profileFragment)
            } else {
                val bundle = Bundle().apply {
                    putString("userId", viewModel.uiState.value.house?.proprietarioId)
                }

                findNavController()
                    .navigate(R.id.action_detailedHouseFragment_to_userProfileFragment, bundle)
            }
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}




