package com.trevit.controller

import com.trevit.dto.AdminDtos.SetWalletRequest
import com.trevit.service.AdminService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 접근 제어는 AdminAuthInterceptor 가 X-Admin-Token 헤더로 처리한다 (여기선 신경 안 씀) */
@RestController
@RequestMapping("/api/admin")
class AdminController(private val admin: AdminService) {

    /** GET /api/admin/status — DB·LLM 연결 상태, 외부 연동 설정 여부, 지갑·통계 */
    @GetMapping("/status")
    fun status() = admin.status()

    /** GET /api/admin/users — 가입자 목록 (비밀번호 해시는 내려주지 않음) */
    @GetMapping("/users")
    fun users() = admin.users()

    /** DELETE /api/admin/users/{id} — 유저 + 로그인 세션 삭제 */
    @DeleteMapping("/users/{id}")
    fun deleteUser(@PathVariable id: Long): Map<String, Boolean> {
        admin.deleteUser(id)
        return mapOf("ok" to true)
    }

    /** POST /api/admin/wallet — 테스트용 지갑 잔액을 임의로 설정 */
    @PostMapping("/wallet")
    fun setWallet(@RequestBody req: SetWalletRequest): Map<String, Long> =
        mapOf("balance" to admin.setWallet(req.balance))

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(e: IllegalArgumentException): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "잘못된 요청")))
}
