package com.trevit.app.map

/**
 * 웹은 현재 위치를 사용하지 않고 null을 반환한다 → 플랜은 숙소를 1일차 출발지로 삼는다.
 *
 * (브라우저 Geolocation은 HTTPS + 사용자 권한 팝업이 필요하고, wasmJs의 js() 는
 *  Kotlin 콜백 전달을 지원하지 않아 네이티브 앱만큼 매끄럽지 않다. 현재 위치 출발은
 *  Android/iOS 앱 전용 기능으로 둔다.)
 */
actual suspend fun getCurrentLocation(): Pair<Double, Double>? = null
