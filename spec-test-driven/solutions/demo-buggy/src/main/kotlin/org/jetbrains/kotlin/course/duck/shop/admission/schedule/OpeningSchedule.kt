package org.jetbrains.kotlin.course.duck.shop.admission.schedule

import java.time.LocalDateTime

// demo-buggy: this part is correct — the only seeded bug is the wrap-around in WindowMatching.kt.
class OpeningSchedule(
    private val windows: List<DailyWindow>,
    private val closures: List<SpecialClosure> = emptyList(),
) {
    fun isOpenAt(at: LocalDateTime): Boolean {
        if (closures.any { it.date == at.toLocalDate() }) return false
        return windows.any { it.covers(at) }
    }
}
