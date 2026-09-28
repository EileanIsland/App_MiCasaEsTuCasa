package com.example.micasaestucasa.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.micasaestucasa.databinding.FragmentBookingBinding
import com.example.micasaestucasa.ui.viewmodel.BookingViewModel

class BookingFragment : Fragment(){
    private val viewModel: BookingViewModel by viewModels()
    private var _binding: FragmentBookingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ){
        super.onViewCreated(view, savedInstanceState)

        val bookId = arguments?.getString("bookId")
        val houseId = arguments?.getString("houseId")

        if(bookId != null){
            viewModel.load(bookId)
        }else if(houseId != null){
            viewModel.load(houseId)
        }

        observeUiState()
        setupListeners()
    }


    fun observeUiState(){

    }

    fun setupListeners(){

    }






}


