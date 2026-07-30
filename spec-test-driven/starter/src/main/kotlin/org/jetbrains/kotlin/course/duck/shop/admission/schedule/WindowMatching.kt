package org.jetbrains.kotlin.course.duck.shop.admission.schedule

import java.time.LocalDateTime

/**
 * Returns `true` if this weekly window covers the instant [at].
 *
 * - Boundary: [DailyWindow.open] is inclusive, [DailyWindow.close] is exclusive — at exactly
 *   `close` the window does NOT cover.
 * - Wrap-around: when `close < open` the window spans midnight, covering `[open, 24:00)` on its
 *   own [DailyWindow.day] and `[00:00, close)` on the following day.
 * - Degenerate: when `close == open` the window covers nothing.
 */
fun DailyWindow.covers(at: LocalDateTime): Boolean = TODO("implement DailyWindow.covers")
