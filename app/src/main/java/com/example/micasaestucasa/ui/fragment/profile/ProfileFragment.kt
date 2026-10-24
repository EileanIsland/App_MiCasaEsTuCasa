package com.example.micasaestucasa.ui.fragment.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.micasaestucasa.R
import com.example.micasaestucasa.databinding.DialogAppearanceBinding
import com.example.micasaestucasa.databinding.FragmentProfileBinding
import com.example.micasaestucasa.ui.adapter.ReviewAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.micasaestucasa.data.local.ThemePreferences
import kotlinx.coroutines.launch

import com.example.micasaestucasa.utils.ViewUtils.manageReviewsDisplay
import com.example.micasaestucasa.utils.ViewUtils.setupUserBadges


class ProfileFragment : Fragment(){
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel : ProfileViewModel by viewModels()
    private lateinit var reviewAdapter: ReviewAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ){
        super.onViewCreated(view, savedInstanceState)

        setUpRecyclerView()
        setUpListeners()
        observeViewModel()

        viewModel.loadProfile()
    }


    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoggedOut) {
                        val intent = requireActivity().intent
                        requireActivity().finish()
                        startActivity(intent)
                        return@collect
                    }

                    Log.d("DEBUG_PROFILE", "stato $state")

                    binding.loadingOverlay.isVisible = state.isLoading
                    binding.userSection.isVisible = !state.isAdmin
                    binding.adminSection.isVisible = state.isAdmin

                    state.user?.let { user ->
                        binding.profileInfo.tvUserName.text =
                            getString(R.string.nome_cognome_user, user.name, user.surname)

                        binding.tvBio.text = user.bio

                        Glide.with(this@ProfileFragment)
                            .load(user.profileImageUrl)
                            .placeholder(R.drawable.ic_person)
                            .circleCrop()
                            .into(binding.profileInfo.ivUserPhoto)

                        binding.chipScrollView.isVisible = user.badges.isNotEmpty()
                        if (user.badges.isNotEmpty()) {
                            binding.profileBadges.setupUserBadges(user.badges)
                        }

                        if (state.isAdmin) {
                            binding.profileInfo.tvUserRole.isVisible = true
                            binding.profileInfo.tvUserRole.text = getString(R.string.administrator)

                            binding.profileInfo.tvUserRating.isVisible = false
                            binding.profileInfo.tvUserStats.isVisible = false
                        } else {
                            // Layout Utente Standard
                            binding.profileInfo.tvUserRole.isVisible = true
                            binding.profileInfo.tvUserRole.text = getString(R.string.proprietario)

                            state.userStats?.let { stats ->
                                binding.profileInfo.tvUserRating.isVisible = true
                                binding.profileInfo.tvUserRating.text = getString(
                                    R.string.rating_num_rec,
                                    stats.ratingMedia,
                                    stats.numeroRecensioni
                                )

                                binding.profileInfo.tvUserStats.isVisible = true
                                binding.profileInfo.tvUserStats.text = getString(
                                    R.string.num_soggiorni_annunci,
                                    stats.numeroSoggiorni,
                                    stats.numeroAnnunci
                                )
                            }

                            // Recensioni (Solo per utenti non admin)
                            manageReviewsDisplay(
                                state.userReviewsReceived,
                                state.isShowingAllReviews,
                                reviewAdapter,
                                binding.reviewsRecyclerView,
                                binding.tvNoReviews,
                                binding.showAllReviewsButton
                            )
                        }

                        // 5. Gestione Errori
                        state.errorMessage?.let { error ->
                            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                            viewModel.errorShown()
                        }
                    }
                }
            }
        }
    }


    private fun setUpRecyclerView(){
        reviewAdapter = ReviewAdapter()

        binding.reviewsRecyclerView.apply{
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setUpListeners(){
        //modifica profilo
        binding.btnEditProfile.setOnClickListener{
            val bundle = Bundle().apply{
                putString("userId", viewModel.uiState.value.user?.id)
            }
            findNavController().navigate(R.id.action_profileFragment_to_modifyProfileFragment, bundle)
        }

        //aspetto
        binding.cardAppearance.setOnClickListener{
            showAppearanceDialog()
        }

        //privacy
        binding.cardPrivacy.setOnClickListener{
            //è messo per riempire, non le implemento
        }

        //bottone log out
        binding.btnLogout.setOnClickListener{
            showLogOutDialog()
        }

        //bottone mostra tutte le recensioni
        binding.showAllReviewsButton.setOnClickListener{
            viewModel.toggleReviews()
        }

    }


    private fun showAppearanceDialog() {
        val themePrefs = ThemePreferences(requireContext())
        val dialogBinding = DialogAppearanceBinding.inflate(layoutInflater)

        when (themePrefs.getTheme()) {
            ThemePreferences.LIGHT -> {
                dialogBinding.radioLight.isChecked = true
            }
            ThemePreferences.DARK -> {
                dialogBinding.radioDark.isChecked = true
            }
            else -> {
                dialogBinding.radioSystem.isChecked = true
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.aspetto))
            .setView(dialogBinding.root)
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Salva") { _, _ ->
                val mode = when (dialogBinding.themeRadioGroup.checkedRadioButtonId) {
                    R.id.radioLight -> ThemePreferences.LIGHT
                    R.id.radioDark -> ThemePreferences.DARK
                    else -> ThemePreferences.SYSTEM
                }

                themePrefs.setTheme(mode)

                Toast.makeText(requireContext(), "Tema aggiornato", Toast.LENGTH_SHORT).show()
            }
            .show()
    }


    //DIALOG PER IL LOGOUT
    private fun showLogOutDialog(){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("logout")
            .setMessage("Vuoi davvero uscire?")
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Esci"){
                    _, _ -> viewModel.logout()
            }
            .show()

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
