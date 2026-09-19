package com.example.micasaestucasa.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.micasaestucasa.databinding.FragmentModifyProfileBinding
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController

class ModifyProfileFragment: Fragment(){

    private var _binding: FragmentModifyProfileBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri : Uri? = null

    private val pickImage =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            uri?.let {
                selectedImageUri = it
                binding.profileImage.setImageURI(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentModifyProfileBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadMockUser()

        setUpListeners()

    }

    private fun loadMockUser(){
        binding.nameEditText.setText("Mario")
        binding.surnameEditText.setText("rossi")
        binding.bioEditText.setText("Ciao sono Mario Rossi")
        //TODO caricare immagine con GLide
    }

    private fun setUpListeners(){
        binding.changePhotoButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.saveProfileButton.setOnClickListener {
            saveProfile()
        }

    }

    private fun validateInput(): Boolean{
        var isValid = true

        if(binding.nameEditText.text.isNullOrBlank()){
            binding.nameEditText.error = "Inserisci il nome"
            isValid = false
        }else{
            binding.nameEditText.error = null
        }

        if(binding.surnameEditText.text.isNullOrBlank()){
            binding.surnameEditText.error = "Inserisci il cognome"
            isValid = false
        }else{
            binding.surnameEditText.error = null
        }

        return isValid
    }

    private fun saveProfile(){
        if(!validateInput()) return

        val name = binding.nameEditText.text.toString().trim()
        val surname = binding.surnameEditText.text.toString().trim()
        val bio = binding.bioEditText.text.toString().trim()

        //TODO
        //Se selectedImageUri != null
        // 1) caricare immagine su Firebase Storage
        // 2) ottenere downloadUrl

        //TODO salvare su Firebase

        Toast.makeText(
            requireContext(),
            "Profilo modificato con successo",
            Toast.LENGTH_SHORT
        ).show()

        findNavController().navigateUp()

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}