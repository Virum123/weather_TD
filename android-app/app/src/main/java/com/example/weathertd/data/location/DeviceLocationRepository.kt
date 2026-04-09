package com.example.weathertd.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

class DeviceLocationRepository(
    private val context: Context,
) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    suspend fun lookupCurrentLocation(): LocationLookupResult {
        if (!context.hasLocationPermission()) {
            return LocationLookupResult.PermissionMissing
        }

        return runCatching {
            val token = CancellationTokenSource()
            val currentLocation = fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token)
                .await()

            val fallbackLocation = if (currentLocation == null) {
                fusedLocationClient.lastLocation.await()
            } else {
                null
            }

            val resolvedLocation = currentLocation ?: fallbackLocation
            if (resolvedLocation == null) {
                LocationLookupResult.Unavailable
            } else {
                LocationLookupResult.Success(
                    latitude = resolvedLocation.latitude,
                    longitude = resolvedLocation.longitude,
                )
            }
        }.getOrElse {
            LocationLookupResult.Unavailable
        }
    }
}

sealed interface LocationLookupResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
    ) : LocationLookupResult

    data object PermissionMissing : LocationLookupResult

    data object Unavailable : LocationLookupResult
}

fun Context.hasLocationPermission(): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
}
