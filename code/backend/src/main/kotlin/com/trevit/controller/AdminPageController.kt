package com.trevit.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * /admin, /admin/ 둘 다 static/admin/index.html 을 돌려준다.
 * Spring의 기본 정적 리소스 핸들러는 확장자 없는 하위 경로를 index.html로
 * 자동 연결해주지 않으므로 직접 서빙한다. (API 토큰 검사는 AdminAuthInterceptor 가
 * 관리자 API만 지키므로 이 페이지 자체는 누구나 열 수 있다 — 데이터는 토큰 없이는 안 보인다)
 */
@RestController
class AdminPageController {

    @GetMapping("/admin", "/admin/")
    fun page(): ResponseEntity<ByteArray> {
        val bytes = ClassPathResource("static/admin/index.html").inputStream.readBytes()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
            .body(bytes)
    }
}
