package com.trevit.app.map

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull

/*
 * 브라우저 Geolocation. wasmJs 의 js() 는 Kotlin 콜백을 넘기기 번거로워서,
 * JS 쪽 watchPosition 이 최신 좌표를 전역 변수에 써 두고 Kotlin 이 짧게 읽어 가는 방식으로 한다.
 * 브라우저는 HTTPS(또는 localhost)에서만 위치를 준다.
 */

private fun startWatch(): Unit = js(
    """{
      if (window.__trevitWatch !== undefined || !navigator.geolocation) return;
      window.__trevitWatch = navigator.geolocation.watchPosition(
        function (p) { window.__trevitPos = [p.coords.latitude, p.coords.longitude]; window.__trevitPosErr = 0; },
        function (e) { window.__trevitPosErr = e.code; },
        { enableHighAccuracy: true, maximumAge: 2000, timeout: 20000 }
      );
    }"""
)

private fun stopWatch(): Unit = js(
    "{ if (window.__trevitWatch !== undefined) { navigator.geolocation.clearWatch(window.__trevitWatch); window.__trevitWatch = undefined; } }"
)

private fun hasPos(): Boolean = js("Array.isArray(window.__trevitPos)")
private fun posLat(): Double = js("window.__trevitPos[0]")
private fun posLng(): Double = js("window.__trevitPos[1]")

private fun currentPos(): Pair<Double, Double>? = if (hasPos()) posLat() to posLng() else null

actual suspend fun getCurrentLocation(): Pair<Double, Double>? {
    startWatch()
    // 권한 팝업에 답할 시간을 조금 준다. 못 받으면 숙소 출발로 폴백
    return withTimeoutOrNull(8_000L) {
        var pos = currentPos()
        while (pos == null) {
            delay(300)
            pos = currentPos()
        }
        pos
    }
}

actual fun locationUpdates(): Flow<Pair<Double, Double>> = flow {
    startWatch()
    var last: Pair<Double, Double>? = null
    try {
        while (true) {
            val pos = currentPos()
            if (pos != null && pos != last) {
                last = pos
                emit(pos)
            }
            delay(1_000)
        }
    } finally {
        stopWatch()
    }
}
