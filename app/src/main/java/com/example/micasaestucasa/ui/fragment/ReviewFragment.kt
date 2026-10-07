package com.example.micasaestucasa.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.micasaestucasa.databinding.FragmentReviewBinding
import com.example.micasaestucasa.ui.viewmodel.ReviewViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ReviewFragment : Fragment() {

    private var _binding: FragmentReviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReviewViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val targetId = arguments?.getString("targetId") ?: ""
        val targetType = arguments?.getString("targetType") ?: ""

        viewModel.initialize(targetId, targetType)

        setupListeners()
        observeUiState()
    }

    private fun setupListeners() {
        binding.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            viewModel.updateRating(rating)
        }

        binding.etComment.doAfterTextChanged { text ->
            viewModel.updateText(text.toString())
        }

        binding.btnPublish.setOnClickListener {
            viewModel.submitReview()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    binding.btnPublish.isEnabled = state.isPublishEnabled && !state.isLoading

                    // Mostra/Nascondi caricamento (opzionale, se hai una ProgressBar)
                    // binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // Gestione Successo
                    if (state.isSuccess) {
                        Toast.makeText(requireContext(), "Recensione pubblicata!", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }

                    // Gestione Errore
                    state.errorMessage?.let { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                        viewModel.clearErrorMessage()
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