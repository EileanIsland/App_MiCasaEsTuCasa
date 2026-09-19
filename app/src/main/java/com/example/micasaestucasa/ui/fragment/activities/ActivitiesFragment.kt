package com.example.micasaestucasa.ui.fragment.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.micasaestucasa.databinding.FragmentActivitiesBinding
import com.google.android.material.tabs.TabLayoutMediator

class ActivitiesFragment : Fragment() {
    private var _binding: FragmentActivitiesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivitiesBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ActivitiesPagerAdapter(this)

        binding.activitiesPager.adapter = adapter

        TabLayoutMediator(
            binding.activitiesTabs,
            binding.activitiesPager
        ) { tab, position ->
            when (position) {
                0 -> tab.text = "Ospite"
                1 -> tab.text = "Proprietario"
            }

        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}