package com.example.micasaestucasa.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.semantics.error
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.micasaestucasa.databinding.FragmentRegistrationBinding
import com.example.micasaestucasa.ui.viewmodel.RegistrationViewModel
import androidx.navigation.fragment.findNavController
import com.example.micasaestucasa.R
import kotlinx.coroutines.launch


class RegistrationFragment : Fragment(){
    private var _binding: FragmentRegistrationBinding? = null
    private val binding get() = _binding!!

    private val viewModel : RegistrationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrationBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }


    //TODO migliorare i messaggi di errore per la password, usare passWOrdInputLayit.error o qualcosa del genere, per ora sono toast message
    private fun observeViewModel(){
        viewLifecycleOwner.lifecycleScope.launch{
            viewModel.uiState.collect { uiState ->

                binding.btnRegister.isEnabled = !uiState.isLoading

                if (uiState.isSucess) {
                    Toast.makeText(requireContext(), "Benvenuto!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_registrationFragment_to_loginFragment)
                }

                uiState.errorMessage?.let{
                    error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                    viewModel.errorShown()
                }

            }
        }
    }


    private fun setupListeners(){
        binding.btnRegister.setOnClickListener{
            val name = binding.etName.text.toString().trim()
            val surname = binding.etSurname.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val conPass = binding.etConfirmPassword.text.toString().trim()

            viewModel.register(name, surname, email, password, conPass)
        }

        binding.tvLoginLink.setOnClickListener{
            findNavController().popBackStack()
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }




}





