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

    private var isReadOnly = false

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

        val bookId = arguments?.getString("bookingId")
        val houseId = arguments?.getString("houseId")
        isReadOnly = arguments?.getBoolean("isReadOnly", false) ?: false

        setupUI()

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

        if(bookId == null && isReadOnly){
            android.util.Log.e("BOOKING_DEBUG", "Errore: bookId mancante!")
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


    private fun setupUI() {
        if (isReadOnly) {
            binding.bookingButton.visibility = View.GONE
            binding.dateCard.isClickable = false
            binding.dateCard.isFocusable = false

            binding.guestsNumber.isEnabled = false
            binding.guestsNumber.inputType = android.text.InputType.TYPE_NULL

            binding.cancelBookingButton.text = "Torna indietro"
        }
    }


    fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // progressBar (TODO implementato)
                    // binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // Il tasto prenota è attivo solo se il form è valido E non siamo in ReadOnly
                    binding.bookingButton.isEnabled = state.isFormValid && !state.isLoading && !isReadOnly

                    // Se lo stato della prenotazione caricata è finale, forziamo il ReadOnly visivo
                    val isFinalState = state.status == "Confermata" || state.status == "Rifiutata"
                    if (isFinalState) {
                        binding.bookingButton.visibility = View.GONE
                        binding.dateCard.isEnabled = false
                        binding.guestsNumber.isEnabled = false
                    }

                    state.casa?.let { casa ->
                        binding.housePreview.apply {
                            tvTitle.text = casa.titolo
                            tvPrice.text = "${casa.prezzoNotte} €/notte"
                            tvCity.text = casa.citta
                            tvGuests.text = casa.ospitiMassimi.toString()
                            tvRating.text = casa.valutazioneMedia.toString()
                            tvRooms.text = casa.numeroCamere.toString()

                            if (casa.immagini.isNotEmpty()) {
                                com.bumptech.glide.Glide.with(this@BookingFragment)
                                    .load(casa.immagini.first())
                                    .centerCrop()
                                    .into(ivHouseCover)
                            }
                        }
                    }

                    // Date
                    binding.checkInDate.text = state.startDate?.let { formatDate(it) } ?: "selezionare data"
                    binding.checkOutDate.text = state.endDate?.let { formatDate(it) } ?: ""

                    // Info prezzi e stato
                    binding.nightsText.text = "Numero notti: ${state.numNights}"
                    binding.totalPrice.text = "Prezzo totale: ${state.totalPrice} €"
                    binding.status.text = "Stato: ${state.status}"

                    // Sincronizzazione numero ospiti
                    val guestsCount = state.numGuest.toString()
                    if (binding.guestsNumber.text.toString() != guestsCount) {
                        binding.guestsNumber.setText(guestsCount)
                    }

                    if (state.isSuccess) {
                        Snackbar.make(requireView(), "Operazione completata!", Snackbar.LENGTH_LONG).show()
                        findNavController().popBackStack()
                    }

                    state.errorMessage?.let {
                        Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }


    fun setupListeners() {
        binding.dateCard.setOnClickListener {
            if (isReadOnly) return@setOnClickListener

            val casa = viewModel.uiState.value.casa
            if (casa != null) {
                val ranges = casa.disponibilita.map { d -> Pair(d.inizio, d.fine) }
                showAvailableRangeDatePicker(childFragmentManager, ranges) { start, end ->
                    viewModel.updateDates(start, end)
                }
            }
        }

        binding.guestsNumber.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !isReadOnly) {
                val guests = binding.guestsNumber.text.toString().toIntOrNull() ?: 1
                viewModel.updateGuest(guests)
            }
        }

        binding.bookingButton.setOnClickListener {
            if (!isReadOnly) {
                viewModel.confirmBooking()
            }
        }

        binding.cancelBookingButton.setOnClickListener {
            if (isReadOnly) {
                findNavController().popBackStack()
            }else{
                val bookId = arguments?.getString("bookingId")
                if (bookId != null) {
                    // dialog conferma
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Annulla Prenotazione")
                        .setMessage("Sei sicuro di voler eliminare definitivamente questa prenotazione?")
                        .setPositiveButton("Elimina") { _, _ ->
                            viewModel.deleteBooking(bookId)
                        }
                        .setNegativeButton("Mantieni", null)
                        .show()
                } else {
                    // Se è una nuova prenotazione non ancora salvata torna  indietro
                    findNavController().popBackStack()
                }
            }
        }
    }

}




