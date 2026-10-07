package com.trevit.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.trevit.dto.PlanDtos.LegDto
import com.trevit.entity.RouteCache
import com.trevit.repository.RouteCacheRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * 경로 API 결과를 DB에 저장해 다시 쓴다 ([RouteCache]).
 * 저장소가 실패해도(DB 연결 끊김 등) 경로 찾기는 계속되어야 하므로 예외는 삼키고 null/무시로 처리한다.
 */
@Service
class RouteCacheStore(
    private val repository: RouteCacheRepository,
    private val mapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(RouteCacheStore::class.java)

    /** 저장된 경로. 없거나 [ttl] 이 지났으면 null */
    fun get(key: String, ttl: Duration): LegDto? = try {
        repository.findById(key).orElse(null)
            ?.takeIf { it.createdAt.isAfter(Instant.now().minus(ttl)) }
            ?.let { mapper.readValue(it.payload, LegDto::class.java) }
    } catch (e: Exception) {
        log.warn("경로 캐시 읽기 실패({}): {}", key, e.message)
        null
    }

    /** 실제 API로 받은 경로만 저장한다 (추정·시각 정보는 저장하지 않는다) */
    fun put(key: String, leg: LegDto) {
        if (leg.estimated) return
        try {
            val clean = leg.copy(departAt = null, arriveAt = null)
            repository.save(RouteCache(key, mapper.writeValueAsString(clean), Instant.now()))
        } catch (e: Exception) {
            log.warn("경로 캐시 저장 실패({}): {}", key, e.message)
        }
    }
}
