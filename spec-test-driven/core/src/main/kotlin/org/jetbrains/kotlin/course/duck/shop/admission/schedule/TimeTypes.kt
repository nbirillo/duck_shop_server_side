package org.jetbrains.kotlin.course.duck.shop.admission.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * A recurring weekly opening window on [day], from [open] (inclusive) to [close] (exclusive).
 *
 * - Normal window: `open < close`, a same-day interval.
 * - Wrap-around window: `close < open`, spanning midnight into the next day (e.g. 22:00–02:00).
 * - Degenerate window: `close == open`, covering nothing.
 *
 * These are plain data; the matching logic lives with the implementation ([DailyWindow.covers]).
 */
data class DailyWindow(val day: DayOfWeek, val open: LocalTime, val close: LocalTime)

/** A specific calendar [date] on which the shop is closed all day, overriding any [DailyWindow]. */
data class SpecialClosure(val date: LocalDate)
