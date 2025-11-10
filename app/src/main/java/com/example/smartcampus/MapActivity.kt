package com.example.smartcampus

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smartcampus.databinding.ActivityMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private lateinit var mMap: GoogleMap
    private val locationPermissionRequestCode = 1
    private val firestore = FirebaseFirestore.getInstance()
    private var allLocations: List<MapLocation> = emptyList()
    private var currentMarker: Marker? = null
    private val TAG = "SMART_CAMPUS_DIAGNOSIS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.fullMapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        addMapOverlay()
        setupSearchView()
        fetchLocationsAndDisplay() // This will now handle the initial search
        enableMyLocation()
    }

    private fun setupSearchView() {
        binding.searchView.isSubmitButtonEnabled = true
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    searchForLocation(query)
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)
                    binding.searchView.clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun searchForLocation(searchText: String) {
        val cleanedSearchText = searchText.trim()
        val matchedLocation = allLocations.find { it.name.contains(cleanedSearchText, ignoreCase = true) }

        currentMarker?.remove()

        if (matchedLocation != null) {
            val position = LatLng(matchedLocation.coordinates.latitude, matchedLocation.coordinates.longitude)

            currentMarker = mMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(matchedLocation.name)
                    .snippet(matchedLocation.details)
            )
            currentMarker?.showInfoWindow()

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 19f))

            LocationDetailSheet.newInstance(matchedLocation).show(supportFragmentManager, "LocationDetailSheet")
        } else {
            Toast.makeText(this, "'$cleanedSearchText' not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addMapOverlay() {
        val southwestBound = LatLng(19.075304, 72.990539)
        val northeastBound = LatLng(19.077231, 72.992517)
        val overlayBounds = LatLngBounds(southwestBound, northeastBound)

        try {
            val groundOverlayOptions = GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.campus_overlay))
                .positionFromBounds(overlayBounds)
                .transparency(0.0f)

            mMap.addGroundOverlay(groundOverlayOptions)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(overlayBounds.center, 17f))

        } catch (e: Exception) {
            Log.e(TAG, "Could not add map overlay.", e)
            Toast.makeText(this, "Error loading campus map overlay.", Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchLocationsAndDisplay() {
        firestore.collection("location")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    return@addOnSuccessListener
                }

                allLocations = documents.toObjects()

                for (location in allLocations) {
                    val latLng = LatLng(location.coordinates.latitude, location.coordinates.longitude)
                    mMap.addCircle(
                        CircleOptions()
                            .center(latLng)
                            .radius(10.0)
                            .strokeColor(Color.argb(255, 0, 123, 255))
                            .fillColor(Color.argb(70, 0, 123, 255))
                            .strokeWidth(2f)
                    )
                }

                // --- THIS IS THE FIX ---
                // After locations are loaded, check if there's a pending search from the chatbot
                intent.getStringExtra("SEARCH_QUERY")?.let { searchQuery ->
                    // Set the query text in the bar and submit it
                    binding.searchView.setQuery(searchQuery, true)
                    // Remove the extra so it doesn't run again on configuration change
                    intent.removeExtra("SEARCH_QUERY")
                }
                // --------------------

            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting documents from Firestore: ", exception)
                Toast.makeText(this, "DATABASE ERROR: Failed to load locations.", Toast.LENGTH_LONG).show()
            }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionRequestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
            }
        }
    }
}