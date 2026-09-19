package com.example.micasaestucasa.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.example.micasaestucasa.ui.viewmodel.ProfileViewModel
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


    private fun observeViewModel(){
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiState.collect{ state->

                    //logout
                    if(state.isLoggedOut){
                        findNavController().navigate(R.id.action_profileFragment_to_LoginFragment){
                            popUpTo(R.id.nav_graph){
                                inclusive = true
                            }
                        }
                    }


                    //Caricamento
                    binding.progressBar.visibility = if(state.isLoading) View.VISIBLE else View.GONE

                    //utente
                    state.user?.let{ user ->
                        binding.profileInfo.tvUserName.text = getString(R.string.nome_cognome_user, user.name, user.surname)

                        binding.tvBio.text = state.user.bio ?: getString(R.string.nessuna_biografia)

                        Glide.with(
                            this@ProfileFragment
                        )
                            .load(user.profileImageUrl)
                            .placeholder(R.drawable.ic_person)
                            .circleCrop()
                            .into(
                                binding.profileInfo.ivUserPhoto
                        )

                        if (user.badge.isNotEmpty()) {
                            binding.profileBadges.visibility = View.VISIBLE
                            binding.profileBadges.setupUserBadges(user.badge)
                        } else {
                            binding.profileBadges.visibility = View.GONE
                        }

                    }

                    //statistiche
                    state.userStats.let{
                        stats ->
                        binding.profileInfo.tvUserRole.visibility = View.VISIBLE
                        binding.profileInfo.tvUserRole.text = getString(R.string.proprietario)

                        binding.profileInfo.tvUserRating.visibility = View.VISIBLE
                        binding.profileInfo.tvUserRating.text = getString(
                            R.string.rating_num_rec,
                            stats?.ratingMedia,
                            stats?.numeroRecensioni
                        )

                        binding.profileInfo.tvUserStats.visibility = View.VISIBLE
                        binding.profileInfo.tvUserStats.text = getString(
                            R.string.num_soggiorni_annunci,
                            stats?.numeroSoggiorni,
                            stats?.numeroAnnunci
                        )
                    }

                    //recensioni
                    manageReviewsDisplay(
                        state.userReviewsReceived,
                        state.isShowingAllReviews,
                        reviewAdapter,
                        binding.reviewsRecyclerView,
                        binding.tvNoReviews,
                        binding.showAllReviewsButton
                    )

                    //gestione errori
                    if(state.errorMessage != null){
                        Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
                        viewModel.errorShown()
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

        //notifiche
        binding.cardNotifications.setOnClickListener{
            //TODO
        }

        //aspetto
        binding.cardAppearance.setOnClickListener{
            showAppearanceDialog()
        }

        //privacy
        binding.cardPrivacy.setOnClickListener{
            //TODO
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


    private fun showAppearanceDialog(){
        val dialogBinding =
            DialogAppearanceBinding.inflate(layoutInflater)


        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Salva"){ _, _ ->

                val theme =
                    when(dialogBinding.themeRadioGroup.checkedRadioButtonId){
                        R.id.radioLight -> "Chiaro"
                        R.id.radioDark -> "Scuro"
                        else -> "Sistema"
                    }

                Toast.makeText(
                    requireContext(),
                    "Tema: $theme",
                    Toast.LENGTH_SHORT
                ).show()

            }
            .show()
        //TODO AppCompatDelegate.setDefaultNightMode(...)

    }

    //DIALOG PER IL LOGOUT
    private fun showLogOutDialog(){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("logout")
            .setMessage("Vuoi davvero uscire?")
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Esci"){
                    _, _ -> viewModel.logout()
                //TODO
            }
            .show()

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
