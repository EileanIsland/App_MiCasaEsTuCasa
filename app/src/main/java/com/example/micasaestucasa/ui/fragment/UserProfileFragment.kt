package com.example.micasaestucasa.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.micasaestucasa.R
import com.example.micasaestucasa.databinding.DialogReportUserBinding
import com.example.micasaestucasa.databinding.FragmentUserProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.micasaestucasa.ui.adapter.ReviewAdapter
import com.example.micasaestucasa.ui.viewmodel.UserProfileViewModel
import kotlinx.coroutines.launch
import com.example.micasaestucasa.utils.ViewUtils.setupUserBadges
import com.example.micasaestucasa.utils.ViewUtils.manageReviewsDisplay
import com.example.micasaestucasa.utils.ViewUtils.showReportUserDialog
import com.google.android.material.snackbar.Snackbar

class UserProfileFragment : Fragment() {

    private var _binding : FragmentUserProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel : UserProfileViewModel by viewModels()
    private lateinit var reviewAdapter: ReviewAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString("userId")

        setUpReviewsRecyclerView()
        setupButtons()
        observeView()

        if(userId != null){
            viewModel.loadUserProfile(userId)
        }else{
            binding.progressBar.visibility = View.GONE
            Snackbar.make(binding.root, "Utente non trovato", Snackbar.LENGTH_LONG).show()
            findNavController().popBackStack()
        }

    }

    private fun observeView(){
        viewLifecycleOwner.lifecycleScope.launch{
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiState.collect {
                    state ->

                    //caricamento
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    //utente
                    state.user?.let{ user ->
                        binding.profileInfo.tvUserName.text = getString(R.string.nome_cognome_user, user.name, user.surname)
                        binding.userBio.text = user.bio

                        Glide.with(
                            this@UserProfileFragment
                        )
                            .load(user.profileImageUrl)
                            .placeholder(R.drawable.ic_person)
                            .circleCrop()
                            .into(
                                binding.profileInfo.ivUserPhoto
                            )

                        binding.badges.setupUserBadges(user.badge)
                    }


                    //statistiche
                    state.userStats?.let{
                            stats ->
                        binding.profileInfo.tvUserRole.visibility = View.GONE

                        binding.profileInfo.tvUserRating.visibility = View.VISIBLE
                        binding.profileInfo.tvUserRating.text = getString(
                            R.string.rating_num_rec,
                            stats.ratingMedia,
                            stats.numeroRecensioni
                        )

                        binding.profileInfo.tvUserStats.visibility = View.VISIBLE
                        binding.profileInfo.tvUserStats.text = getString(
                            R.string.num_soggiorni_annunci,
                            stats.numeroSoggiorni,
                            stats.numeroAnnunci
                        )
                    }


                    //RECENSIONI
                    manageReviewsDisplay(
                        state.reviews,
                        state.isShowingAllReviews,
                        reviewAdapter,
                        binding.reviewsRecyclerView,
                        binding.tvNoReviews,
                        binding.showAllReviewsButton
                    )

                    state.feedback?.let{
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                        viewModel.clearFeedback()
                    }

                    state.errorMessage?.let { error ->
                        Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                        viewModel.clearError()
                    }

                }

            }
        }

    }


    private fun setUpReviewsRecyclerView() {
        reviewAdapter = ReviewAdapter()

        binding.reviewsRecyclerView.apply{
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewAdapter
            isNestedScrollingEnabled = false
        }

    }

    private fun setupButtons() {
        //SEGNALARE UTENTE
        binding.reportButton.setOnClickListener {
            showReportUserDialog(requireContext(), layoutInflater) { reason ->
                viewModel.reportUser(reason)
            }
        }

        //Contattare utente
        binding.contactButton.setOnClickListener {
            val currentUserId = viewModel.uiState.value.user?.id
            if (currentUserId != null) {
                val bundle = Bundle().apply {
                    putString("otherUserId", currentUserId)
                }
                findNavController().navigate(
                    R.id.action_userProfileFragment_to_detailedChatFragment,
                    bundle
                )
            }
        }

        //bottone mostra tutte le recensioni
        binding.showAllReviewsButton.setOnClickListener {
            viewModel.toggleReviews()
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }
}



