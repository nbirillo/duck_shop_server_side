package org.jetbrains.kotlin.course.duck.shop.admission.schedule

import java.time.LocalDateTime

/** Reference implementation (teacher-only, :grading). */
class OpeningSchedule(
    private val windows: List<DailyWindow>,
    private val closures: List<SpecialClosure> = emptyList(),
) {
    fun isOpenAt(at: LocalDateTime): Boolean {
        if (closures.any { it.date == at.toLocalDate() }) return false
        return windows.any { it.covers(at) }
    }
}
