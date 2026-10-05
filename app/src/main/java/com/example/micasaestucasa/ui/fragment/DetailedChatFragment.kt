package com.example.micasaestucasa.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.compose.ui.semantics.text
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.micasaestucasa.databinding.FragmentDetailedChatBinding
import com.example.micasaestucasa.ui.adapter.MessageAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch






