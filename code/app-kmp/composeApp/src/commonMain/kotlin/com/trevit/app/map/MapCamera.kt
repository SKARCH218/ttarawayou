package com.trevit.app.map

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/** 웹 메르카토르(OSM) 타일 좌표계 상수 */
const val TILE_SIZE = 256

data class TileKey(val z: Int, val x: Int, val y: Int)

fun lngToWorldX(lng: Double, zoom: Int): Double =
    (lng + 180.0) / 360.0 * TILE_SIZE * (1 shl zoom).toDouble()

fun latToWorldY(lat: Double, zoom: Int): Double {
    val rad = lat * PI / 180.0
    return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * TILE_SIZE * (1 shl zoom).toDouble()
}

/**
 * 내비게이션 카메라 — 사용자 위치를 항상 화면 중앙에 두는 고정 줌 투영.
 * 확대/이동 제스처는 두지 않는다 (미스터리 지도 규칙: 줌 잠금).
 */
class MapCamera(
    centerLat: Double,
    centerLng: Double,
    val zoom: Int,
    private val tileScale: Float,
    private val screenW: Float,
    private val screenH: Float,
) {
    private val cx = lngToWorldX(centerLng, zoom)
    private val cy = latToWorldY(centerLat, zoom)

    /** 타일 한 장이 화면에서 차지하는 픽셀 크기 */
    val tileRenderPx: Float get() = TILE_SIZE * tileScale

    fun toOffset(lat: Double, lng: Double): Offset = Offset(
        ((lngToWorldX(lng, zoom) - cx) * tileScale + screenW / 2.0).toFloat(),
        ((latToWorldY(lat, zoom) - cy) * tileScale + screenH / 2.0).toFloat(),
    )

    /** 타일 (x,y)의 화면상 좌상단 좌표 */
    fun tileTopLeft(x: Int, y: Int): Offset = Offset(
        ((x.toDouble() * TILE_SIZE - cx) * tileScale + screenW / 2.0).toFloat(),
        ((y.toDouble() * TILE_SIZE - cy) * tileScale + screenH / 2.0).toFloat(),
    )

    /** 화면을 덮는 데 필요한 타일 목록 */
    fun visibleTiles(): List<TileKey> {
        val max = (1 shl zoom) - 1
        val halfW = screenW / 2.0 / tileScale
        val halfH = screenH / 2.0 / tileScale
        val x0 = floor((cx - halfW) / TILE_SIZE).toInt().coerceIn(0, max)
        val x1 = floor((cx + halfW) / TILE_SIZE).toInt().coerceIn(0, max)
        val y0 = floor((cy - halfH) / TILE_SIZE).toInt().coerceIn(0, max)
        val y1 = floor((cy + halfH) / TILE_SIZE).toInt().coerceIn(0, max)
        return buildList {
            for (x in x0..x1) for (y in y0..y1) add(TileKey(zoom, x, y))
        }
    }
}
