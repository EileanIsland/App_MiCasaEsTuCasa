package com.example.micasaestucasa.ui.fragment.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.micasaestucasa.R
import androidx.navigation.fragment.findNavController
import com.example.micasaestucasa.MainActivity
import com.example.micasaestucasa.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch


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
        setUpClickableRegisterText()
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

                        val options = android.app.ActivityOptions.makeCustomAnimation(
                            requireContext(),
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                        )

                        startActivity(intent, options.toBundle())
                    }

                    state.errorMessage?.let { error ->
                        Snackbar.make(
                            binding.root,
                            error,
                            Snackbar.LENGTH_LONG
                        ).show()

                        viewModel.errorShown() }
                    }

                }
            }

    }


    private fun setUpClickableRegisterText() {
        val fullText = getString(R.string.frase_registrazione)
        val clickablePart = getString(R.string.register_clickable_part)

        val spannableString = SpannableString(fullText)

        val startIndex = fullText.indexOf(clickablePart)
        val endIndex = startIndex + clickablePart.length

        if (startIndex != -1) {
            val colorPrimary =
                ContextCompat.getColor(requireContext(), R.color.secondary_orange)
            spannableString.setSpan(
                ForegroundColorSpan(colorPrimary),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            }

            spannableString.setSpan(
                clickableSpan,
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )


        }
        binding.tvRegisterLink.apply {
            text = spannableString
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT
        }
    }


    private fun setupButtons(){
        //Bottone login
        binding.btnLogin.setOnClickListener {
            hideKeyboard()

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if(email.isNotEmpty() && password.isNotEmpty()){
                viewModel.login(email, password)

            }else{
                Snackbar.make(
                    binding.root,
                    "Inserisci email e password",
                    Snackbar.LENGTH_LONG
                ).show()
            }
            binding.etPassword.text?.clear()

        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val currentFocusedView = requireActivity().currentFocus
        currentFocusedView?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}




