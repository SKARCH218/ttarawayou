package com.trevit.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 프론트엔드 설정 제공 — 지도 SDK용 TMAP 앱키를 내려준다.
 * 키를 .env(run-backend) 한 곳에만 넣으면 웹 지도까지 함께 동작하게 하기 위함.
 */
@RestController
@RequestMapping("/api/config")
class ConfigController(@Value("\${tmap.app-key}") tmapAppKey: String?) {

    private val tmapAppKey: String = tmapAppKey?.trim() ?: ""

    @GetMapping
    fun config(): Map<String, String> = mapOf("tmapAppKey" to tmapAppKey)
}
