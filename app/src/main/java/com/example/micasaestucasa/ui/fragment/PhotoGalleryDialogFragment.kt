package com.example.micasaestucasa.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.micasaestucasa.databinding.DialogPhotoGalleryBinding
import com.example.micasaestucasa.ui.adapter.FullScreenPhotoAdapter


class PhotoGalleryDialogFragment : DialogFragment() {
    private var _binding: DialogPhotoGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var images: List<String>
    private var startPosition: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        images = arguments?.getStringArrayList(ARG_IMAGES) ?: emptyList()

        startPosition = arguments?.getInt(ARG_POSITION) ?: 0
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            DialogPhotoGalleryBinding.inflate(
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
        setupViewPager()

        binding.closeButton.setOnClickListener {
            dismiss()
        }

    }


    private fun setupViewPager(){
        val adapter = FullScreenPhotoAdapter()
        binding.photosViewPager.adapter = adapter
        adapter.submitList(images)

        binding.photosViewPager.setCurrentItem(
            startPosition,
            false
        )
    }


    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {

            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            setBackgroundDrawableResource(
                android.R.color.transparent
            )
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }


    companion object {
        private const val ARG_IMAGES = "images"

        private const val ARG_POSITION = "position"


        fun newInstance(
            images: List<String>,
            position: Int
        ): PhotoGalleryDialogFragment {


            return PhotoGalleryDialogFragment().apply {

                arguments = Bundle().apply {

                    putStringArrayList(
                        ARG_IMAGES,
                        ArrayList(images)
                    )

                    putInt(
                        ARG_POSITION,
                        position
                    )
                }
            }
        }
    }
}