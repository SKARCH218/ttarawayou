package com.trevit.app.map

/**
 * 현재 위치(위도, 경도)를 반환한다. 권한이 없거나 위치를 못 얻으면 null.
 * null이면 플랜은 숙소를 1일차 출발지로 삼는다(기존 동작).
 */
expect suspend fun getCurrentLocation(): Pair<Double, Double>?
