package com.example.micasaestucasa.ui.fragment.auth

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.micasaestucasa.databinding.FragmentRegistrationBinding
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

        setUpClickableLoginText()
        setupListeners()
        observeViewModel()
    }


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

    }


    private fun setUpClickableLoginText() {
        val fullText = getString(R.string.hai_gia_un_account)
        val clickablePart = getString(R.string.login_clickable_part)

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
                    findNavController().navigate(R.id.action_registrationFragment_to_loginFragment)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                    ds.color = colorPrimary
                    ds.isFakeBoldText = true
                }
            }

            spannableString.setSpan(
                clickableSpan,
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )


        }
        binding.tvLoginLink.apply {
            text = spannableString
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }




}





