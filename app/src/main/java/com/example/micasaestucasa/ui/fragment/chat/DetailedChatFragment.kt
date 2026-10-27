package com.example.micasaestucasa.ui.fragment.chat

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
import com.example.micasaestucasa.ui.fragment.PhotoGalleryDialogFragment
import com.example.micasaestucasa.utils.ViewUtils.showReportUserDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        val state = viewModel.uiState.value
        val isHost = state.isHost

        messageAdapter = MessageAdapter(
            currentUid = viewModel.currentUserID ?: "",
            onImageClick = { imageUrl ->
                PhotoGalleryDialogFragment.Companion.newInstance(arrayListOf(imageUrl), 0)
                    .show(childFragmentManager, "photoGallery")
            },
            onContextClick = { id, type ->
                handleContextClick(id, type)
            },

            onReviewHouse = if (isHost) null else { bId ->
                navigateToReview(state.house?.id ?: "", bId, "CASA")
            },

            onReviewUser = { bId ->
                val currentState = viewModel.uiState.value
                val targetUserId = currentState.otherUser?.id

                if (!targetUserId.isNullOrEmpty()) {
                    navigateToReview(targetUserId, bId, "UTENTE")
                }
            }
        )

        binding.rvMessages.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
        }
    }

    private fun navigateToReview(targetId: String, bookingId: String, targetType: String) {
        val bundle = Bundle().apply {
            putString("targetId", targetId)
            putString("bookingId", bookingId)
            putString("targetType", targetType)
        }
        findNavController().navigate(R.id.reviewFragment, bundle)
    }

    private fun handleContextClick(id: String, type: String) {
        val bundle = Bundle()
        val state = viewModel.uiState.value
        when (type.uppercase()) {
            "HOUSE" -> {
                bundle.putString("houseId", id)
                findNavController().navigate(R.id.detailedHouseFragment, bundle)
            }
            "BOOKING" -> {
                val houseId = state.house?.id ?: state.bookingsMap[id]?.idCasa
                bundle.putString("bookingId", id)
                bundle.putBoolean("isReadOnly", true)
                bundle.putString("houseId", houseId)
                findNavController().navigate(R.id.bookingFragment, bundle)
            }
        }
    }

    private fun setupToolbarListeners() {
        binding.btnReportChat.setOnClickListener {
            showReportUserDialog(requireContext(), layoutInflater) { reason ->
                viewModel.reportUser(reason)
            }
        }

        val navigateToProfile = View.OnClickListener {
            val state = viewModel.uiState.value
            val bundle = Bundle().apply { putString("userId", state.otherUser?.id) }
            findNavController().navigate(R.id.userProfileFragment, bundle)
        }
        binding.tvChatUserName.setOnClickListener(navigateToProfile)
        binding.ivUserProfile.setOnClickListener(navigateToProfile)

        binding.btnDeleteChat.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Elimina chat")
                .setMessage("Sei sicuro di voler eliminare questa conversazione?")
                .setPositiveButton("Elimina") { _, _ -> viewModel.deleteChat() }
                .setNegativeButton("Annulla", null)
                .show()
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

                    // 1. SINCRONIZZA I DATI DELLE PRENOTAZIONI CON L'ADAPTER
                    messageAdapter.updateBookings(state.bookingsMap)

                    // 2. Carica i messaggi
                    messageAdapter.submitList(state.messages) {
                        if (state.messages.isNotEmpty()) {
                            binding.rvMessages.scrollToPosition(state.messages.size - 1)
                        }
                    }

                    // 3. Info Toolbar
                    state.otherUser?.let { user ->
                        binding.tvChatUserName.text = "${user.name} ${user.surname}"
                        Glide.with(this@DetailedChatFragment)
                            .load(user.profileImageUrl)
                            .placeholder(R.drawable.ic_person)
                            .circleCrop()
                            .into(binding.ivUserProfile)
                    }

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




