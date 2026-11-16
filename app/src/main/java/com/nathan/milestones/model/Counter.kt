package com.nathan.milestones.model

import java.time.LocalDate

/**
 * LIVE      = changes every day, e.g. "2 months, 1 day"
 * MILESTONE = only changes when a full span passes, e.g. "2 months"
 */
enum class CountMode { LIVE, MILESTONE }

enum class CountUnit(val singular: String, val plural: String, val label: String) {
    DAYS("day", "days", "Days"),
    WEEKS("week", "weeks", "Weeks"),
    MONTHS("month", "months", "Months"),
    YEARS("year", "years", "Years")
}

/** One special date, e.g. "❤️ Us" starting Sep 13. */
data class Counter(
    val id: String,
    val title: String,
    val startDate: LocalDate,
    val mode: CountMode,
    val unit: CountUnit,
    /**
     * Your own celebration text. Blank = default "Happy 2 months!".
     * {n} = the number, {unit} = month/months etc, {title} = the date's name.
     */
    val customMessage: String = "",
    /** Send a notification on milestone days */
    val notify: Boolean = true,
    /** Also send a heads-up the day before a milestone */
    val notifyDayBefore: Boolean = false,
    /** Shown before the title, e.g. "❤️ Us" */
    val emoji: String = "❤️",
    /** Which preset colour from CounterColors.all (0 = Pink). Ignored if customColor is set. */
    val colorIndex: Int = 0,
    /** A colour you picked yourself (0xAARRGGBB), or null to use the preset */
    val customColor: Long? = null
)