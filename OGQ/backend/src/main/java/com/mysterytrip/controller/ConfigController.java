package com.mysterytrip.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 프론트엔드 설정 제공 — 지도 SDK용 TMAP 앱키를 내려준다.
 * 키를 run-backend.bat 한 곳에만 넣으면 웹 지도까지 함께 동작하게 하기 위함.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final String tmapAppKey;

    public ConfigController(@Value("${tmap.app-key}") String tmapAppKey) {
        this.tmapAppKey = tmapAppKey == null ? "" : tmapAppKey.trim();
    }

    @GetMapping
    public Map<String, String> config() {
        return Map.of("tmapAppKey", tmapAppKey);
    }
}
