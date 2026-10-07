package com.example.micasaestucasa.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.micasaestucasa.databinding.FragmentDetailedChatBinding
import com.example.micasaestucasa.ui.adapter.MessageAdapter
import com.example.micasaestucasa.ui.viewmodel.DetailedChatViewModel
import com.example.micasaestucasa.utils.ViewUtils.showReportUserDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import com.example.micasaestucasa.R

class DetailedChatFragment : Fragment(){

    private var _binding: FragmentDetailedChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailedChatViewModel by viewModels()

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let{ viewModel.sendImageMessage(it.toString()) }
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailedChatBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        val messageAdapter = MessageAdapter(
            currentUid = viewModel.currentUserID ?: "",
            onImageClick = { imageUrl ->
                val images = arrayListOf(imageUrl)
                PhotoGalleryDialogFragment.newInstance(
                    images,
                    0
                ).show(
                    childFragmentManager,
                    "photoGallery"
                )
            }
        )

        binding.rvMessages.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupToolbarListeners() {
        binding.btnReportChat.setOnClickListener {
            showReportUserDialog(requireContext(), layoutInflater) { reason ->
                viewModel.reportUser(reason)
            }
        }

        binding.tvChatUserName.setOnClickListener {
            val state = viewModel.uiState.value
            val bundle = Bundle().apply {
                putString("userId", state.otherUser?.id)
            }
            findNavController().navigate(com.example.micasaestucasa.R.id.userProfileFragment, bundle)

        }

        binding.ivUserProfile.setOnClickListener {
            val state = viewModel.uiState.value
            val bundle = Bundle().apply {
                putString("userId", state.otherUser?.id)
            }
            findNavController().navigate(com.example.micasaestucasa.R.id.userProfileFragment, bundle)

        }

        binding.btnDeleteChat.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Elimina chat")
                .setMessage("Sei sicuro di voler eliminare questa conversazione?")
                .setPositiveButton("Elimina") { _, _ -> viewModel.deleteChat() }
                .setNegativeButton("Annulla", null)
                .show()
        }

        binding.chatDetailToolbar.findViewById<View>(com.example.micasaestucasa.R.id.btnRateUser)?.setOnClickListener {
            val state = viewModel.uiState.value
            val bundle = Bundle().apply {
                putString("targetId", state.otherUser?.id)
                putString("targetType", "utente")
            }
            findNavController().navigate(com.example.micasaestucasa.R.id.reviewFragment, bundle)
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

                    (binding.rvMessages.adapter as? MessageAdapter)?.submitList(state.messages) {
                        if (state.messages.isNotEmpty()) {
                            binding.rvMessages.scrollToPosition(state.messages.size - 1)
                        }
                    }

                    state.otherUser?.let { user ->
                        binding.tvChatUserName.text = "${user.name} ${user.surname}"
                        Glide.with(this@DetailedChatFragment)
                            .load(user.profileImageUrl)
                            .placeholder(com.example.micasaestucasa.R.drawable.ic_person)
                            .circleCrop()
                            .into(binding.ivUserProfile)
                    }

                    state.house?.let { house ->
                        binding.chatHeaderInfo.isVisible = true
                        binding.tvContextTitle.text = house.titolo

                        Glide.with(this@DetailedChatFragment)
                            .load(house.immagini.firstOrNull()).centerCrop()
                            .into(binding.ivContextHouse)

                        if(state.booking != null){
                            binding.tvContextSubtitle.text = "Vedi i dettagli della prenotazione"
                            binding.chatHeaderInfo.setOnClickListener {
                                val bundle = Bundle().apply {
                                    putString("bookingId", state.booking.idBooking)
                                    putString("houseId", house.id)
                                    putBoolean("isReadOnly", true)
                                }
                                findNavController().navigate(com.example.micasaestucasa.R.id.bookingFragment, bundle)
                            }
                        }else {
                            binding.tvContextSubtitle.text = "In riferimento a questo annuncio"

                            binding.chatHeaderInfo.setOnClickListener {
                                val bundle = Bundle().apply {
                                    putString("houseId", house.id)
                                }
                                findNavController().navigate(com.example.micasaestucasa.R.id.detailedHouseFragment, bundle)
                            }
                        }



                    } ?: run {
                        binding.chatHeaderInfo.isVisible = false
                    }


                    // Gestione visibilità icona recensione
                    binding.chatDetailToolbar.findViewById<View>(com.example.micasaestucasa.R.id.btnRateUser)?.isVisible =
                        state.canReview && !state.hasAlreadyReviewed

                    // Gestione Feedback e Errori (Snackbar)
                    state.userFeedback?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                        viewModel.clearFeedback()
                    }

                    state.errorMessage?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                        viewModel.clearError()
                    }

                    //Navigazione in caso di successo (es. chat eliminata)
                    if (state.actionSuccess) {
                        findNavController().popBackStack()
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






