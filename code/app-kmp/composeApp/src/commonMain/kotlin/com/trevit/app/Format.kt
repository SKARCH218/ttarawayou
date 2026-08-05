package com.trevit.app

import kotlin.math.abs
import kotlin.math.round

// JVM String.format 은 wasm(웹) 타깃에 없으므로 공용 숫자 포맷을 직접 제공한다

/** "%,d" — 천 단위 콤마 */
fun comma(value: Long): String {
    val negative = value < 0
    val digits = abs(value).toString()
    val sb = StringBuilder()
    for ((i, c) in digits.withIndex()) {
        if (i > 0 && (digits.length - i) % 3 == 0) sb.append(',')
        sb.append(c)
    }
    return if (negative) "-$sb" else sb.toString()
}

fun comma(value: Int): String = comma(value.toLong())

/** "%.1f" — 소수 첫째 자리 반올림 (음수 미지원, 별점·거리 전용) */
fun oneDecimal(value: Double): String {
    val scaled = round(value * 10).toLong()
    return "${scaled / 10}.${abs(scaled % 10)}"
}

/** "%02d" */
fun twoDigits(value: Int): String = value.toString().padStart(2, '0')
