package org.jetbrains.kotlin.course.duck.shop.admission.schedule

import java.time.LocalDateTime

/** Reference implementation (teacher-only, :grading). */
fun DailyWindow.covers(at: LocalDateTime): Boolean {
    val time = at.toLocalTime()
    val d = at.dayOfWeek
    return when {
        open == close -> false                                        // degenerate: empty
        open < close -> d == day && time >= open && time < close      // normal same-day window
        else -> (d == day && time >= open) ||                         // wrap: late on its own day
            (d == day.plus(1) && time < close)                        // ...and early next day
    }
}
