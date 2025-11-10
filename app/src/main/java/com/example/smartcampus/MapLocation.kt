package com.example.smartcampus

import com.google.firebase.firestore.GeoPoint

data class MapLocation(
    // We add default values to prevent crashes if a field is missing in Firestore
    val name: String = "",
    val category: String = "",
    val details: String = "",
    val photoUrl: String = "",
    val floorMapUrl: String = "",
    val coordinates: GeoPoint = GeoPoint(0.0, 0.0) // Default to (0,0)
)