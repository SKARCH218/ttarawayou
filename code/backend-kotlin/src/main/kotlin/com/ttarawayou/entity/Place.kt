package com.ttarawayou.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "places")
class Place(
    @Column(nullable = false)
    var name: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: PlaceType = PlaceType.ATTRACTION,

    @Column(nullable = false)
    var address: String = "",

    @Column(nullable = false)
    var latitude: Double = 0.0,

    @Column(nullable = false)
    var longitude: Double = 0.0,

    /** LODGING: 1박 요금(원), RESTAURANT: 1인 평균 식사 가격(원), ATTRACTION: 1인 입장료(원) */
    @Column(nullable = false)
    var price: Int = 0,

    @Column(nullable = false)
    var rating: Double = 0.0,

    var description: String? = null,
) {
    enum class PlaceType { LODGING, RESTAURANT, ATTRACTION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** TMAP·시드 등 외부 소스에서 만든 비영속 장소에 식별자를 부여할 때도 사용 */
    var id: Long? = null

    /** 취향 필터용 태그 (산/바다/공원/강/실내 등) — 시드 데이터에서 부여, JSON 미노출 */
    @jakarta.persistence.Transient
    @get:com.fasterxml.jackson.annotation.JsonIgnore
    var tags: Set<String> = emptySet()
}
