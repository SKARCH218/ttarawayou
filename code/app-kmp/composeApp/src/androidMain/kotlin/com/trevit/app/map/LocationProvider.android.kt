package com.trevit.app.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** MainActivity에서 applicationContext를 넣어 둔다 (멀티플랫폼 위치 조회용) */
object AndroidAppContext {
    var context: Context? = null
}

@SuppressLint("MissingPermission")
actual suspend fun getCurrentLocation(): Pair<Double, Double>? {
    val ctx = AndroidAppContext.context ?: return null

    val fine = ctx.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ctx.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
        return null
    }

    val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

    // 1) 캐시된 마지막 위치가 있으면 즉시 사용 (가장 빠름)
    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    )
    providers
        .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
        .maxByOrNull { it.time }
        ?.let { return it.latitude to it.longitude }

    // 2) 마지막 위치가 없으면(부팅 직후 등) 새 위치를 한 번 요청해 짧게 기다린다
    val activeProvider = when {
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> return null
    }
    val fresh = withTimeoutOrNull(9_000L) {
        suspendCancellableCoroutine<Location?> { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    lm.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }
                override fun onProviderDisabled(provider: String) {}
                override fun onProviderEnabled(provider: String) {}
                @Deprecated("deprecated in API 29")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            }
            lm.requestLocationUpdates(activeProvider, 0L, 0f, listener, Looper.getMainLooper())
            cont.invokeOnCancellation { lm.removeUpdates(listener) }
        }
    }
    return fresh?.let { it.latitude to it.longitude }
}
