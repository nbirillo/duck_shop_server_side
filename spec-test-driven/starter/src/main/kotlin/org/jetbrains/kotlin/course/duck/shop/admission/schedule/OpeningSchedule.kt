package org.jetbrains.kotlin.course.duck.shop.admission.schedule

import java.time.LocalDateTime

/**
 * A weekly opening schedule made of recurring [windows] and date-specific [closures].
 */
class OpeningSchedule(
    private val windows: List<DailyWindow>,
    private val closures: List<SpecialClosure> = emptyList(),
) {
    /**
     * Returns `true` if the shop is open at [at].
     *
     * A [SpecialClosure] whose date equals `at`'s date closes the shop all day, overriding every
     * window. Otherwise the shop is open iff at least one [windows] entry [covers] [at]. With no
     * matching window — including an empty schedule — the shop is closed.
     */
    fun isOpenAt(at: LocalDateTime): Boolean = TODO("implement OpeningSchedule.isOpenAt")
}
