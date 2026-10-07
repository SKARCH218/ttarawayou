package com.trevit.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 외부 경로 API(TMAP·ODsay·OSRM) 결과 저장소.
 * 대중교통 API는 하루 호출량이 작아 같은 구간을 매번 부르면 금방 바닥난다(429).
 * 한 번 받은 경로를 저장해 두고 서버 재시작·서버 간에도 다시 쓴다.
 */
@Entity
@Table(name = "route_cache")
class RouteCache(
    /** "T:37.56650,126.97800>37.57960,126.97700" — 구간 종류 + 출발·도착 좌표(약 1m 단위) */
    @Id
    @Column(length = 96)
    var cacheKey: String = "",

    /** LegDto JSON (시각 정보 없는 원본 경로) */
    @Column(nullable = false, columnDefinition = "text")
    var payload: String = "",

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),
)
