package com.example.site_survey.services

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class GpsLocationService(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun locationFlow() = callbackFlow<Location> {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .setMinUpdateDistanceMeters(1f)
            .build()

        val callback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }

        client.requestLocationUpdates(request, callback, context.mainLooper)
        awaitClose { client.removeLocationUpdates(callback) }
    }
}