package com.example.micasaestucasa.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.micasaestucasa.R
import com.example.micasaestucasa.databinding.FragmentChatBinding
import com.example.micasaestucasa.ui.adapter.ChatAdapter
import com.example.micasaestucasa.ui.viewmodel.ChatViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch


class ChatFragment: Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()

    private lateinit var chatAdapter : ChatAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setUpListeners()
        observeUiState()
    }


    fun observeUiState(){
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading

                    binding.rvChats.isVisible = !state.isLoading && state.chats.isNotEmpty()
                    binding.noResult.isVisible = !state.isLoading && state.chats.isEmpty()


                    if(state.chats.isNotEmpty()){
                        binding.rvChats.isVisible = true
                        binding.noResult.isVisible = false
                        chatAdapter.submitList(state.chats)
                    }else{
                        binding.rvChats.isVisible = false
                        binding.noResult.isVisible = true
                    }

                    if(state.errorMessage != null){
                        Log.e("CHAT_DEBUG", state.errorMessage)
                        Snackbar.make(
                            requireView(),
                            state.errorMessage,
                            Snackbar.LENGTH_LONG
                        ).show()
                        viewModel.errorShown()
                    }

                }
            }
        }


    }

    fun setupRecyclerView(){
        chatAdapter = ChatAdapter{ chat->
            val bundle = bundleOf(
                "chatId" to chat.id,
                "otherUserId" to chat.otherUserId
            )
            findNavController().navigate(R.id.action_chatFragment_to_detailedChatFragment, bundle)

        }


        binding.rvChats.apply{
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

    }

    fun setUpListeners(){
        //TODO
    //      aggiungere azioni per cancellare chat
    //      segnalare utente


    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }




}
