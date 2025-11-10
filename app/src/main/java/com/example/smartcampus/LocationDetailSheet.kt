package com.example.smartcampus

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.smartcampus.databinding.BottomSheetLocationDetailBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.parcelize.Parcelize // <-- THIS IMPORT IS THE FIX

// Helper data class because GeoPoint isn't Parcelable
@Parcelize
data class SimpleLocation(
    val name: String,
    val details: String,
    val photoUrl: String
) : Parcelable

class LocationDetailSheet : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetLocationDetailBinding
    private var simpleLocation: SimpleLocation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Updated to work with modern Android versions
            simpleLocation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable("location", SimpleLocation::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable("location")
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetLocationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        simpleLocation?.let {
            binding.locationSheetName.text = it.name
            binding.locationSheetDetails.text = it.details
            Glide.with(this).load(it.photoUrl).into(binding.locationSheetImage)
        }
    }

    companion object {
        fun newInstance(location: MapLocation): LocationDetailSheet {
            val args = Bundle().apply {
                val simpleLoc = SimpleLocation(
                    name = location.name,
                    details = location.details,
                    photoUrl = location.photoUrl
                )
                putParcelable("location", simpleLoc)
            }
            return LocationDetailSheet().apply {
                arguments = args
            }
        }
    }
}