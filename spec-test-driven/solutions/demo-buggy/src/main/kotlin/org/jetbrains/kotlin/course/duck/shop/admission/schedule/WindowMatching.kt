package org.jetbrains.kotlin.course.duck.shop.admission.schedule

import java.time.LocalDateTime

// demo-buggy: SEEDED bug — treats every window as a plain same-day interval and IGNORES the
// wrap-around-past-midnight case, so late-night windows like 22:00–02:00 are handled wrong.
fun DailyWindow.covers(at: LocalDateTime): Boolean {
    val time = at.toLocalTime()
    return at.dayOfWeek == day && time >= open && time < close
}
