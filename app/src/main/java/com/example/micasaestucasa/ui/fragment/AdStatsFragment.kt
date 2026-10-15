package com.example.micasaestucasa.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.micasaestucasa.databinding.FragmentAdStatsBinding
import com.example.micasaestucasa.databinding.ItemStatBinding
import com.example.micasaestucasa.ui.viewmodel.AdStatsViewMod
import kotlinx.coroutines.launch

class AdStatsFragment : Fragment() {
    private var _binding: FragmentAdStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdStatsViewMod by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdStatsBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                android.util.Log.d("AdStatsFragment", "UI State: $state")
                binding.tvTotalValue.text = state.total.toString()

                updateStatCard(binding.cardConcluded, "Concluse", state.concluded)
                updateStatCard(binding.cardOngoing, "In Corso", state.ongoing)
                updateStatCard(binding.cardUpcoming, "Da Iniziare", state.upcoming)
                updateStatCard(binding.cardHouses, "In Attesa", state.pending) // Usiamo l'ID esistente cardHouses
            }
        }
    }

    private fun updateStatCard(includedLayout: ItemStatBinding, label: String, value: Int) {
        includedLayout.tvStatLabel.text = label
        includedLayout.tvStatValue.text = value.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}