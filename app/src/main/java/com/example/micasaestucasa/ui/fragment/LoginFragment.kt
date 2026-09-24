package com.example.micasaestucasa.ui.fragment

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
import com.example.micasaestucasa.databinding.FragmentLoginBinding
import com.example.micasaestucasa.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

//TODO
// 3 - autorizzazione per admin


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
                        Toast.makeText(requireContext(), "Accesso eseguito!", Toast.LENGTH_SHORT).show()
                        val destination = if (state.isAdmin) {
                            R.id.action_loginFragment_to_adminFragment
                        } else {
                            R.id.action_loginFragment_to_homeFragment
                        }
                        findNavController().navigate(destination)
                    }

                    state.errorMessage?.let { error ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                        viewModel.errorShown()
                    }

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

        //TODO
        /*
        val stringaCompleta = getString(R.string.testo_registrazione)
        val parolaCliccabile = "Registrati qui"

        val spannableString = SpannableString(stringaCompleta)
        val startIndex = stringaCompleta.indexOf(parolaCliccabile)
        val endIndex = startIndex + parolaCliccabile.length

        // Creiamo il link d'azione
        val clickableSpan = object : ClickableSpan() {
        override fun onClick(widget: View) {
        // Inserisci qui l'azione che vuoi far fare al click!
        navController.navigate(R.id.registerFragment)
        }
        }

        // Applichiamo il click alla parola specifica
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.myTextView.text = spannableString
        binding.myTextView.movementMethod = LinkMovementMethod.getInstance()

         */

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}




