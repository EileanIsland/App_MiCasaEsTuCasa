package com.example.micasaestucasa.ui.fragment

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
import com.example.micasaestucasa.databinding.FragmentBookingBinding
import com.example.micasaestucasa.ui.viewmodel.BookingViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import com.example.micasaestucasa.utils.DateUtils.formatDate
import com.example.micasaestucasa.utils.ViewUtils.showAvailableRangeDatePicker


//TODO:
// 1 - mettere scritte vicino ai dati es: selezionare data 2
// 2 - item_house iniziale non carica tutti i dati (mi sono dimenticata di mettere il binding)
// 3 - validazione dettagli prenotazione
//      (controllare che il numero di ospiti non supera il numero di max di ospiti
//      date prenotazione siano corrette eccetera -
// 4 -mi ha accettato pretnotazione con input 40 ospiti (errore) anche se poi ha salvato con 3 capire come mai


class BookingFragment : Fragment(){
    private val viewModel: BookingViewModel by viewModels()
    private var _binding: FragmentBookingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ){
        super.onViewCreated(view, savedInstanceState)

        val bookId = arguments?.getString("bookId")
        val houseId = arguments?.getString("houseId")

        if(houseId == null){
            android.util.Log.e("BOOKING_DEBUG", "Errore: houseId mancante!")
            Snackbar.make(
                view,
                "Errore nel caricamento della prenotazione!",
                Snackbar.LENGTH_LONG
            ).show()

            findNavController().popBackStack()
            return
        }

        viewModel.loadBookingData(houseId, bookId)

        observeUiState()
        setupListeners()
    }


    fun observeUiState(){
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiState.collect { state ->
                    //TODO aggiungere la progressBar al file xml
                    //binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.bookingButton.isEnabled = state.isFormValid && !state.isLoading

                    state.casa?.let{casa->
                        binding.housePreview.tvTitle.text = casa.titolo
                        binding.housePreview.tvPrice.text = "${casa.prezzoNotte} €/notte"
                        binding.housePreview.tvCity.text = casa.citta
                        binding.housePreview.tvGuests.text = casa.ospitiMassimi.toString()
                        binding.housePreview.tvRating.text = casa.valutazioneMedia.toString()
                        binding.housePreview.tvRooms.text = casa.numeroCamere.toString()


                        if (casa.immagini.isNotEmpty()) {
                            com.bumptech.glide.Glide.with(this@BookingFragment)
                                .load(casa.immagini.first())
                                .centerCrop()
                                .into(binding.housePreview.ivHouseCover)
                        }

                    }

                    //date
                    binding.checkInDate.text = state.startDate?.let{formatDate(it) } ?: "selezionare data"
                    binding.checkOutDate.text = state.endDate?.let{formatDate(it)}

                    //info ospiti
                    binding.nightsText.text = "Numero notti: " + state.numNights.toString()
                    binding.totalPrice.text = "Prezzo totale: "  + state.totalPrice.toString()
                    binding.status.text = "Stato: " + state.status

                    val guestsCount = state.numGuest.toString()
                    if (binding.guestsNumber.text.toString() != guestsCount) {
                        binding.guestsNumber.setText(guestsCount)
                        binding.guestsNumber.setSelection(guestsCount.length)
                    }
                    if(state.isSuccess){
                        Snackbar.make(
                            requireView(),
                            "Prenotazione avvenuta con successo!",
                            Snackbar.LENGTH_LONG
                        ).show()
                        findNavController().popBackStack()
                    }


                    if(state.errorMessage != null){
                        Snackbar.make(
                            requireView(),
                            state.errorMessage,
                            Snackbar.LENGTH_LONG
                        ).show()
                        viewModel.clearError()
                    }

                }

            }

        }

    }

    fun setupListeners(){
        binding.dateCard.setOnClickListener {
            val casa = viewModel.uiState.value.casa
            if (casa != null) {
                val ranges = casa.disponibilita.map { d -> Pair(d.inizio, d.fine) }

                showAvailableRangeDatePicker(childFragmentManager, ranges) { start, end ->
                    viewModel.updateDates(start, end)
                }
            } else {
                Snackbar.make(binding.root, "Dati casa non pronti", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.guestsNumber.setOnClickListener {
            val guests = binding.guestsNumber.text.toString().toIntOrNull() ?: 1
            viewModel.updateGuest(guests)
        }

        // PRENOTA
        binding.bookingButton.setOnClickListener {
            viewModel.confirmBooking()
        }

        //cancella
        binding.cancelBookingButton.setOnClickListener {
            findNavController().popBackStack()
        }

    }

}




