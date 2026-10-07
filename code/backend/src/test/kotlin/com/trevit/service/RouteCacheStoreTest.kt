package com.trevit.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.trevit.dto.PlanDtos.LegDto
import com.trevit.dto.PlanDtos.StepDto
import com.trevit.entity.RouteCache
import com.trevit.repository.RouteCacheRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import java.time.Duration
import java.time.Instant
import java.util.Optional

/** 경로 캐시: 받은 경로를 그대로 되살리고, 추정 경로·만료된 경로는 쓰지 않는다 */
class RouteCacheStoreTest {

    private val saved = HashMap<String, RouteCache>()
    private val repo: RouteCacheRepository = Mockito.mock(RouteCacheRepository::class.java).also { r ->
        Mockito.`when`(r.save(any(RouteCache::class.java))).thenAnswer { inv ->
            (inv.arguments[0] as RouteCache).also { saved[it.cacheKey] = it }
        }
        Mockito.`when`(r.findById(Mockito.anyString())).thenAnswer { inv ->
            Optional.ofNullable(saved[inv.arguments[0] as String])
        }
    }
    private val store = RouteCacheStore(repo, jacksonObjectMapper())

    private val busLeg = LegDto(
        "TRANSIT", 1900.0, 14, 1500, "강남08번 버스 (A 승차 → B 하차, 3개 정류장)",
        listOf(doubleArrayOf(37.51, 127.04), doubleArrayOf(37.52, 127.05)),
        boardStop = "A", alightStop = "B", departAt = "10:00", arriveAt = "10:14",
        boardLat = 37.511, boardLng = 127.041, alightLat = 37.519, alightLng = 127.049,
        stations = listOf(doubleArrayOf(37.511, 127.041), doubleArrayOf(37.519, 127.049)),
        steps = listOf(StepDto("BUS", "강남08번 버스 · A 승차 → B 하차 (3개 정류장)", 1700.0, 10)),
    )

    @Test
    fun `저장한 경로를 그대로 되살린다 (시각은 빼고)`() {
        store.put("T:1", busLeg)
        val back = store.get("T:1", Duration.ofDays(30))
        assertNotNull(back)
        assertEquals(busLeg.summary, back!!.summary)
        assertEquals(busLeg.steps, back.steps)
        assertEquals(busLeg.boardLat, back.boardLat)
        assertEquals(busLeg.path.map { it.toList() }, back.path.map { it.toList() })
        assertEquals(busLeg.stations!!.map { it.toList() }, back.stations!!.map { it.toList() })
        assertNull(back.departAt, "일정 시각은 저장하지 않아야 함")
    }

    @Test
    fun `추정 경로는 저장하지 않는다`() {
        store.put("T:2", busLeg.copy(estimated = true))
        assertNull(store.get("T:2", Duration.ofDays(30)))
    }

    @Test
    fun `만료된 경로는 쓰지 않는다`() {
        store.put("T:3", busLeg)
        saved["T:3"]!!.createdAt = Instant.now().minus(Duration.ofDays(31))
        assertNull(store.get("T:3", Duration.ofDays(30)))
    }
}
