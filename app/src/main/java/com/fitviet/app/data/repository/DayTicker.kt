package com.fitviet.app.data.repository

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Emits today's date, then re-emits at each local midnight — shared by repositories whose
 * "today"-relative queries would otherwise freeze at whatever date they were first collected on
 * (see Gate 3 PROGRESS.md). DST-safe: uses `ZonedDateTime`/`Duration`, not naive `LocalDateTime`.
 */
internal fun dayTicker(zone: ZoneId): Flow<LocalDate> = flow {
    while (true) {
        val now = ZonedDateTime.now(zone)
        emit(now.toLocalDate())
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zone)
        delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000))
    }
}
