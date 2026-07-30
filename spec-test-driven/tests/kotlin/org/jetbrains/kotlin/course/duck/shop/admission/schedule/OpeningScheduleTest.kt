package org.jetbrains.kotlin.course.duck.shop.admission.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Shared acceptance suite for the opening-schedule engine ([DailyWindow.covers] +
 * [OpeningSchedule.isOpenAt]). Compiled and run against every implementation module.
 *
 * Reference calendar: 2026-01-06 = Tuesday, 2026-01-07 = Wednesday, 2026-01-13 = Tuesday,
 * 2026-01-02 = Friday, 2026-01-03 = Saturday.
 */
class OpeningScheduleTest {

    private fun at(date: String, time: String) =
        LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time))

    private val tueBusiness = DailyWindow(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(18, 0))
    private val friNight = DailyWindow(DayOfWeek.FRIDAY, LocalTime.of(22, 0), LocalTime.of(2, 0))

    // ---- normal window + boundaries ----

    @Test
    fun `open inside a normal window`() {
        assertTrue(OpeningSchedule(listOf(tueBusiness)).isOpenAt(at("2026-01-06", "12:00")))
    }

    @Test
    fun `open exactly at the start (inclusive)`() {
        assertTrue(OpeningSchedule(listOf(tueBusiness)).isOpenAt(at("2026-01-06", "09:00")))
    }

    @Test
    fun `closed exactly at the end (exclusive)`() {
        assertFalse(OpeningSchedule(listOf(tueBusiness)).isOpenAt(at("2026-01-06", "18:00")))
    }

    @Test
    fun `closed before opening and on another day`() {
        val s = OpeningSchedule(listOf(tueBusiness))
        assertFalse(s.isOpenAt(at("2026-01-06", "08:59"))) // before open
        assertFalse(s.isOpenAt(at("2026-01-07", "12:00"))) // Wednesday
    }

    // ---- wrap-around past midnight ----

    @Test
    fun `wrap-around covers late night on its own day`() {
        assertTrue(OpeningSchedule(listOf(friNight)).isOpenAt(at("2026-01-02", "23:00")))
    }

    @Test
    fun `wrap-around covers early morning of the next day`() {
        assertTrue(OpeningSchedule(listOf(friNight)).isOpenAt(at("2026-01-03", "01:00"))) // Saturday
    }

    @Test
    fun `wrap-around closed at the exclusive end next day`() {
        assertFalse(OpeningSchedule(listOf(friNight)).isOpenAt(at("2026-01-03", "02:00")))
    }

    @Test
    fun `wrap-around closed before it starts`() {
        assertFalse(OpeningSchedule(listOf(friNight)).isOpenAt(at("2026-01-02", "21:00")))
    }

    @Test
    fun `wrap-around does not open on an unrelated day`() {
        val s = OpeningSchedule(listOf(friNight)) // FRIDAY 22:00–02:00 only
        assertFalse(s.isOpenAt(at("2026-01-06", "23:00"))) // Tuesday night — not this window's day
        assertFalse(s.isOpenAt(at("2026-01-07", "01:00"))) // Wednesday small hours — not the day after Friday
    }

    // ---- special closures ----

    @Test
    fun `special closure overrides an open window`() {
        val s = OpeningSchedule(
            listOf(tueBusiness),
            listOf(SpecialClosure(LocalDate.parse("2026-01-06"))),
        )
        assertFalse(s.isOpenAt(at("2026-01-06", "12:00"))) // closed this Tuesday
        assertTrue(s.isOpenAt(at("2026-01-13", "12:00")))  // open the next Tuesday
    }

    // ---- union + empty + degenerate ----

    @Test
    fun `empty schedule is always closed`() {
        assertFalse(OpeningSchedule(emptyList()).isOpenAt(at("2026-01-06", "12:00")))
    }

    @Test
    fun `union of two windows on the same day`() {
        val morning = DailyWindow(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0))
        val evening = DailyWindow(DayOfWeek.TUESDAY, LocalTime.of(17, 0), LocalTime.of(20, 0))
        val s = OpeningSchedule(listOf(morning, evening))
        assertTrue(s.isOpenAt(at("2026-01-06", "10:00")))
        assertTrue(s.isOpenAt(at("2026-01-06", "18:00")))
        assertFalse(s.isOpenAt(at("2026-01-06", "15:00"))) // gap between windows
    }

    @Test
    fun `degenerate window covers nothing`() {
        val zero = DailyWindow(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(9, 0))
        assertFalse(OpeningSchedule(listOf(zero)).isOpenAt(at("2026-01-06", "09:00")))
    }
}
