package com.example.micasaestucasa.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.micasaestucasa.databinding.FragmentModifyProfileBinding
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.micasaestucasa.R
import com.example.micasaestucasa.ui.viewmodel.ModifyProfileViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ModifyProfileFragment: Fragment(){

    private var _binding: FragmentModifyProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ModifyProfileViewModel by viewModels()

    private var pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let{
            viewModel.onImageSelected(it.toString())

            Glide.with(this)
                .load(it)
                .circleCrop()
                .into(binding.profileImage)

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModifyProfileBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpListeners()
        observeUiState()
    }

    private fun observeUiState(){
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.saveProfileButton.isEnabled = !state.isLoading

                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    if (state.isLoading) {
                        binding.mainContainer.alpha = 0.5f
                        binding.saveProfileButton.isEnabled = false
                    } else {
                        binding.mainContainer.alpha = 1.0f
                        binding.saveProfileButton.isEnabled = true
                    }

                    if (binding.nameEditText.text.isNullOrEmpty() && state.name.isNotEmpty()) {
                        binding.nameEditText.setText(state.name)
                        binding.surnameEditText.setText(state.surname)
                        binding.bioEditText.setText(state.bio)
                    }

                    if (state.newImageUri == null && state.profileImageUrl.isNotEmpty()) {
                        binding.profileImage.imageTintList = null

                        Glide.with(this@ModifyProfileFragment)
                            .load(state.profileImageUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(binding.profileImage)
                    }

                    if (state.isSuccess) {
                        Snackbar.make(binding.root, "Profilo aggiornato con successo", Snackbar.LENGTH_LONG).show()
                        viewModel.resetSuccess()
                        findNavController().popBackStack()
                    }

                    state.errorMessage?.let { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }

    }



    private fun setUpListeners() {
        binding.changePhotoButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.saveProfileButton.setOnClickListener {
            if (validateInput()) {
                viewModel.updateName(binding.nameEditText.text.toString())
                viewModel.updateSurname(binding.surnameEditText.text.toString())
                viewModel.updateBio(binding.bioEditText.text.toString())
                viewModel.updatePhoneNumber(binding.phoneEditText.text.toString())

                viewModel.saveProfile()
            }
        }
    }


    private fun validateInput(): Boolean {
        var isValid = true

        if (binding.nameEditText.text.isNullOrBlank()) {
            binding.nameEditText.error = "Inserisci il nome"
            isValid = false
        }

        if (binding.surnameEditText.text.isNullOrBlank()) {
            binding.surnameEditText.error = "Inserisci il cognome"
            isValid = false
        }

        return isValid
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}





