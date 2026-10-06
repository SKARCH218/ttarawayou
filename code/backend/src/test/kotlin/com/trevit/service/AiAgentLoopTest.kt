package com.trevit.service

import com.sun.net.httpserver.HttpServer
import com.trevit.entity.Place
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

/**
 * AI 도구 호출 루프: AI가 search_places 를 부르면 서버가 검색해 결과를 돌려주고,
 * AI는 그 결과의 id로 최종 일정을 낸다. (OpenAI 호환 가짜 서버로 검증)
 */
class AiAgentLoopTest {

    @Test
    fun `AI가 도구로 찾은 장소를 최종 일정에 쓴다`() {
        val bodies = CopyOnWriteArrayList<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/chat/completions") { ex ->
            val body = ex.requestBody.readBytes().toString(Charsets.UTF_8)
            bodies += body
            val reply = if (bodies.size == 1) {
                """{"choices":[{"message":{"role":"assistant","content":"","tool_calls":[
                  {"id":"call_1","type":"function","function":{"name":"search_places",
                   "arguments":"{\"query\":\"루프탑 카페\",\"kind\":\"restaurant\",\"near_place_id\":1}"}}]}}]}"""
            } else {
                """{"choices":[{"message":{"role":"assistant",
                  "content":"{\"lodgingId\":0,\"days\":[{\"stopIds\":[1,777]}],\"reason\":\"루프탑 카페를 찾아 넣었어요\"}"}}]}"""
            }
            val bytes = reply.toByteArray()
            ex.sendResponseHeaders(200, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val ai = AiPlanService(
                "http://127.0.0.1:${server.address.port}/v1", "test-model", true, 5000, "", 1000,
                agentEnabled = true, maxToolTurns = 4,
            )
            val sight = Place("경복궁", Place.PlaceType.ATTRACTION, "서울 종로구", 37.5796, 126.9770, 9000, 4.7, "")
                .also { it.id = 1 }
            val searched = mutableListOf<String>()
            val search = AiPlanService.PlaceSearch { query, type, _, _, _ ->
                searched += "$query/$type"
                listOf(Place("하늘 루프탑", type, "서울 종로구", 37.5800, 126.9780, 9000, 4.5, "").also { it.id = 777 })
            }

            val sel = ai.plan(21_000, 1, 1, 0, listOf(sight), search = search, anchor = 37.58 to 126.977)

            assertNotNull(sel, "AI 결과가 버려졌음")
            assertEquals(listOf(1L, 777L), sel!!.days[0].map { it.id })
            assertEquals(listOf("루프탑 카페/RESTAURANT"), searched)
            assertEquals(2, bodies.size)
            assertTrue(bodies[0].contains("\"tools\""), "첫 요청에 도구 정의가 없음")
            assertTrue(bodies[1].contains("\"tool_call_id\":\"call_1\"") && bodies[1].contains("하늘 루프탑"),
                "도구 결과가 두 번째 요청에 실리지 않음")
            println("도구 루프 OK — 검색: $searched, 최종 일정: ${sel.days[0].map { it.name }}")
        } finally {
            server.stop(0)
        }
    }
}
