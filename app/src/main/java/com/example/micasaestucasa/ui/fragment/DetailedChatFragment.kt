package com.example.micasaestucasa.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.micasaestucasa.R
import com.example.micasaestucasa.databinding.FragmentDetailedChatBinding
import com.example.micasaestucasa.ui.adapter.MessageAdapter
import com.example.micasaestucasa.ui.viewmodel.DetailedChatViewModel
import com.example.micasaestucasa.utils.ViewUtils.showReportUserDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class DetailedChatFragment : Fragment() {

    private var _binding: FragmentDetailedChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailedChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.sendImageMessage(it.toString()) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailedChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recupero argomenti senza SafeArgs
        val chatId = arguments?.getString("chatId")
        val otherUserId = arguments?.getString("otherUserId") ?: ""
        val casaId = arguments?.getString("casaId")
        val bookingId = arguments?.getString("bookingId")

        setupRecyclerView()
        setupToolbarListeners()
        setupInputListeners()
        observeUiState()

        viewModel.initChat(chatId, otherUserId, casaId, bookingId)
    }

    private fun setupRecyclerView() {
        // Inizializziamo l'adapter con le 3 azioni: Click immagine, Click Card Contesto
        messageAdapter = MessageAdapter(
            currentUid = viewModel.currentUserID ?: "",
            onImageClick = { imageUrl ->
                PhotoGalleryDialogFragment.newInstance(arrayListOf(imageUrl), 0)
                    .show(childFragmentManager, "photoGallery")
            },

            onContextClick = { id, type ->
                android.util.Log.d("NAV_DEBUG", "Cliccato contesto: ID=$id, TYPE=$type")
                val bundle = Bundle()
                val state = viewModel.uiState.value
                android.util.Log.d("NAV_DEBUG", "ID casa: $state")


                when (type.uppercase()) {
                    "HOUSE" -> {
                        bundle.putString("houseId", id)
                        findNavController().navigate(R.id.detailedHouseFragment, bundle)
                    }
                    "BOOKING" -> {
                        val houseId = state.house?.id ?: state.booking?.idCasa
                        android.util.Log.d("NAV_DEBUG", "ID casa: $houseId")
                        bundle.putString("bookingId", id)
                        bundle.putBoolean("isReadOnly", true)
                        bundle.putString("houseId", houseId)
                        findNavController().navigate(R.id.bookingFragment, bundle)
                    }
                    else -> android.util.Log.e("NAV_DEBUG", "Tipo di contesto sconosciuto: $type")
                }
            }

        )

        binding.rvMessages.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true // I messaggi partono dal basso
            }
        }
    }

    private fun setupToolbarListeners() {
        binding.btnReportChat.setOnClickListener {
            showReportUserDialog(requireContext(), layoutInflater) { reason ->
                viewModel.reportUser(reason)
            }
        }

        // Navigazione al profilo dell'altro utente
        val navigateToProfile = View.OnClickListener {
            val state = viewModel.uiState.value
            val bundle = Bundle().apply { putString("userId", state.otherUser?.id) }
            findNavController().navigate(R.id.userProfileFragment, bundle)
        }
        binding.tvChatUserName.setOnClickListener(navigateToProfile)
        binding.ivUserProfile.setOnClickListener(navigateToProfile)

        binding.btnDeleteChat.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Elimina chat")
                .setMessage("Sei sicuro di voler eliminare questa conversazione?")
                .setPositiveButton("Elimina") { _, _ -> viewModel.deleteChat() }
                .setNegativeButton("Annulla", null)
                .show()
        }

        // Tasto stella per recensione (Toolbar)
        binding.btnRateUser.setOnClickListener {
            val state = viewModel.uiState.value
            val bundle = Bundle().apply {
                putString("targetId", state.otherUser?.id)
                putString("targetType", "UTENTE")
            }
            findNavController().navigate(R.id.reviewFragment, bundle)
        }
    }

    private fun setupInputListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                binding.etMessage.text?.clear()
            }
        }

        binding.btnAttach.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    // 1. Aggiornamento lista messaggi (ora include la Card di contesto)
                    messageAdapter.submitList(state.messages) {
                        if (state.messages.isNotEmpty()) {
                            binding.rvMessages.scrollToPosition(state.messages.size - 1)
                        }
                    }

                    // 2. Toolbar (Dati utente)
                    state.otherUser?.let { user ->
                        binding.tvChatUserName.text = "${user.name} ${user.surname}"
                        Glide.with(this@DetailedChatFragment)
                            .load(user.profileImageUrl)
                            .placeholder(R.drawable.ic_person)
                            .circleCrop()
                            .into(binding.ivUserProfile)
                    }

                    // 3. Stella recensione (Toolbar)
                    binding.btnRateUser.visibility = if (state.canReview && !state.hasAlreadyReviewed)
                        View.VISIBLE else View.GONE

                    // 4. Feedback e Errori
                    state.userFeedback?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                        viewModel.clearFeedback()
                    }

                    state.errorMessage?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                        viewModel.clearError()
                    }

                    if (state.actionSuccess) findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}





