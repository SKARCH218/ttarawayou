package com.trevit.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * 관리자 API(/api/admin 이하) 전체를 X-Admin-Token 헤더로 지킨다.
 * ADMIN_TOKEN 이 설정돼 있지 않으면(기본값) 관리자 기능 자체를 꺼둔다 —
 * 다른 시크릿들과 같은 원칙: 값이 없으면 "안전하게 비활성", 추측 가능한 기본 비밀번호를 쓰지 않는다.
 */
@Component
class AdminAuthInterceptor(
    @Value("\${admin.token:}") private val adminToken: String,
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (adminToken.isBlank()) {
            reject(response, 503, "관리자 기능이 꺼져 있어요. 서버에 ADMIN_TOKEN 을 설정하세요.")
            return false
        }
        val given = request.getHeader("X-Admin-Token")
        if (given != adminToken) {
            reject(response, 401, "관리자 토큰이 올바르지 않아요.")
            return false
        }
        return true
    }

    private fun reject(response: HttpServletResponse, status: Int, message: String) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write("""{"message":"$message"}""")
    }
}

@org.springframework.context.annotation.Configuration
class AdminInterceptorConfig(private val adminAuthInterceptor: AdminAuthInterceptor) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(adminAuthInterceptor).addPathPatterns("/api/admin/**")
    }
}
