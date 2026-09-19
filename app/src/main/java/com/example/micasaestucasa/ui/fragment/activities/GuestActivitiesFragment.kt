package com.example.micasaestucasa.ui.fragment.activities

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.micasaestucasa.R
import com.example.micasaestucasa.databinding.FragmentGuestActivitiesBinding

class GuestActivitiesFragment : Fragment(){
    private var _binding: FragmentGuestActivitiesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGuestActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //BOOKING ADAPTER
    }

    override fun onDestroyView(){
        super.onDestroyView()
        _binding = null
    }

}