package com.trevit.app.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** MainActivity에서 applicationContext를 넣어 둔다 (멀티플랫폼 위치 조회용) */
object AndroidAppContext {
    var context: Context? = null
    /** MainActivity 가 넣어 둔다 — 실시간 모드에서 권한이 없을 때 요청 창을 다시 띄운다 */
    var requestLocationPermission: (() -> Unit)? = null
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

private fun hasLocationPermission(ctx: Context): Boolean =
    ctx.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ctx.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/**
 * 실시간 위치. 권한이 없으면 권한 요청 창을 다시 띄우고 허용될 때까지 기다린다
 * (앱을 처음 켤 때 거절했어도 실시간 모드에서 다시 켤 수 있게).
 * 시작하자마자 마지막으로 알려진 위치를 먼저 내보내 "GPS 대기"가 길지 않게 한다.
 */
@SuppressLint("MissingPermission")
actual fun locationUpdates(): Flow<Pair<Double, Double>> = flow {
    val ctx = AndroidAppContext.context ?: return@flow
    if (!hasLocationPermission(ctx)) {
        AndroidAppContext.requestLocationPermission?.invoke()
        while (!hasLocationPermission(ctx)) delay(1_000)
    }
    val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@flow
    listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
        ?.let { emit(it.latitude to it.longitude) }
    emitAll(callbackFlow {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.latitude to location.longitude)
            }
            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            @Deprecated("deprecated in API 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }
        // GPS가 정확하지만 실내에선 안 잡히므로 네트워크 위치도 같이 받는다
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
            .forEach { lm.requestLocationUpdates(it, 1_000L, 1f, listener, Looper.getMainLooper()) }
        awaitClose { lm.removeUpdates(listener) }
    })
}
