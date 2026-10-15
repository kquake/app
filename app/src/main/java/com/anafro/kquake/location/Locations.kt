package com.anafro.kquake.location

import android.Manifest
import android.location.Location
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private val SUPPORTED_PROVIDERS = setOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

/**
 * Streams location fixes from the GPS and network providers, starting with the freshest last-known fix.
 *
 * Uses the framework LocationManager rather than Play Services so it also works on devices without GMS.
 * Each fix carries [Location.getElapsedRealtimeNanos], which shares a time base with accelerometer
 * timestamps, so readings from both sources can be aligned.
 */
@RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
fun LocationManager.locationUpdates(intervalMillis: Long = 5_000): Flow<Location> = callbackFlow {
    val providers = getProviders(true).filter { it in SUPPORTED_PROVIDERS }

    providers.mapNotNull { getLastKnownLocation(it) }
        .maxByOrNull { it.elapsedRealtimeNanos }
        ?.let { trySend(it) }

    val request = LocationRequestCompat.Builder(intervalMillis)
        .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
        .build()
    val listener = LocationListenerCompat { trySend(it) }
    for (provider in providers) {
        LocationManagerCompat.requestLocationUpdates(this@locationUpdates, provider, request, Runnable::run, listener)
    }

    awaitClose { LocationManagerCompat.removeUpdates(this@locationUpdates, listener) }
}
