package com.example.micasaestucasa.ui.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.micasaestucasa.R
import androidx.navigation.fragment.findNavController
import com.example.micasaestucasa.MainActivity
import com.example.micasaestucasa.databinding.FragmentLoginBinding
import com.example.micasaestucasa.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

//TODO
// 3 - autorizzazione per admin
// 4 - implementare errori validazione con TextInputLayout.Error se voglio evidenziare un campo
    // snackbar per errori


class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.btnLogin.isEnabled = !state.isLoading

                    binding.btnLogin.isEnabled = !state.isLoading

                    //SUCCESSO
                    if (state.isSuccess) {
                        val intent = Intent(requireContext(), MainActivity::class.java)

                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)

                        requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

                    }

                    state.errorMessage?.let { error ->
                        com.google.android.material.snackbar.Snackbar.make(        binding.root,
                            error,
                            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                        ).show()

                        viewModel.errorShown() }
                    }

                }
            }

    }


    private fun setupButtons(){
        //Bottone login
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if(email.isNotEmpty() && password.isNotEmpty()){
                viewModel.login(email, password)

            }else{
                Toast.makeText(requireContext(), "Inserisci email e password", Toast.LENGTH_SHORT).show()
            }
            binding.etPassword.text?.clear()

        }

        //link registrazione
        binding.tvRegisterLink.setOnClickListener {
            findNavController().navigate(
                R.id.action_loginFragment_to_registerFragment
            )
        }


    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}




