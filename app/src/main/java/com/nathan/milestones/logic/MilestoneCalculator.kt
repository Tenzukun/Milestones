package com.nathan.milestones.logic

import com.nathan.milestones.model.CountMode
import com.nathan.milestones.model.CountUnit
import com.nathan.milestones.model.Counter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Everything the screens and the widget need to show for one counter. */
data class CounterDisplay(
    val headline: String,        // "2 months" or "62 days"
    val detail: String,          // "2 months, 1 day" or "Since Sep 13, 2026"
    val celebration: String?,    // "Happy 1 month!" on a milestone day, else null
    val nextMilestone: String?,  // "Next: 3 months on Dec 13, 2026 (in 29 days)"
    val nextShort: String?,      // "3 months in 29 days" (short version for widgets)
    val modeLabel: String        // "Live" or "Updates every month"
)

/** A milestone that lands on a specific date, e.g. 3 months. */
data class Milestone(val count: Long, val unit: CountUnit) {
    val label: String get() = MilestoneCalculator.plural(count, unit)
}

/** One row in the "Upcoming" list. */
data class UpcomingMilestone(
    val date: LocalDate,
    val milestone: Milestone,
    val daysUntil: Long
)

object MilestoneCalculator {

    private val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val dayFormat = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

    fun formatDate(date: LocalDate): String = date.format(dateFormat)
    fun formatDateWithDay(date: LocalDate): String = date.format(dayFormat)

    /** "1 day", "2 days", "1 month" ... */
    fun plural(n: Long, unit: CountUnit): String =
        "$n ${if (n == 1L) unit.singular else unit.plural}"

    /**
     * Number of full months between start and today.
     * Uses start.plusMonths(n) so a Jan 31 start counts Feb 28 as "1 month"
     * (Java moves it to the last day of shorter months).
     */
    fun completedMonths(start: LocalDate, today: LocalDate): Long {
        if (today.isBefore(start)) return 0
        var n = ChronoUnit.MONTHS.between(start, today)
        while (!start.plusMonths(n + 1).isAfter(today)) n++
        while (n > 0 && start.plusMonths(n).isAfter(today)) n--
        return n
    }

    fun completedUnits(start: LocalDate, today: LocalDate, unit: CountUnit): Long {
        val days = ChronoUnit.DAYS.between(start, today)
        return when (unit) {
            CountUnit.DAYS -> days
            CountUnit.WEEKS -> days / 7
            CountUnit.MONTHS -> completedMonths(start, today)
            CountUnit.YEARS -> completedMonths(start, today) / 12
        }
    }

    /** The date of the n-th milestone, e.g. n = 3 months after start. */
    fun milestoneDate(start: LocalDate, unit: CountUnit, n: Long): LocalDate = when (unit) {
        CountUnit.DAYS -> start.plusDays(n)
        CountUnit.WEEKS -> start.plusWeeks(n)
        CountUnit.MONTHS -> start.plusMonths(n)
        CountUnit.YEARS -> start.plusMonths(n * 12)
    }

    /** Live breakdown, e.g. "1 year, 2 months, 3 days" or "8 weeks, 6 days". */
    fun breakdown(start: LocalDate, today: LocalDate, unit: CountUnit): String {
        val totalDays = ChronoUnit.DAYS.between(start, today)
        val parts = mutableListOf<String>()
        when (unit) {
            CountUnit.DAYS -> parts += plural(totalDays, CountUnit.DAYS)
            CountUnit.WEEKS -> {
                val w = totalDays / 7
                val d = totalDays % 7
                if (w > 0) parts += plural(w, CountUnit.WEEKS)
                if (d > 0) parts += plural(d, CountUnit.DAYS)
            }
            CountUnit.MONTHS, CountUnit.YEARS -> {
                val totalMonths = completedMonths(start, today)
                val d = ChronoUnit.DAYS.between(start.plusMonths(totalMonths), today)
                if (unit == CountUnit.YEARS) {
                    val y = totalMonths / 12
                    val m = totalMonths % 12
                    if (y > 0) parts += plural(y, CountUnit.YEARS)
                    if (m > 0) parts += plural(m, CountUnit.MONTHS)
                } else if (totalMonths > 0) {
                    parts += plural(totalMonths, CountUnit.MONTHS)
                }
                if (d > 0) parts += plural(d, CountUnit.DAYS)
            }
        }
        return if (parts.isEmpty()) plural(0, CountUnit.DAYS) else parts.joinToString(", ")
    }

    /**
     * Which milestone (if any) falls on [date]. If two land on the same day:
     * years > every 100 days > months > weeks.
     */
    fun milestoneOn(start: LocalDate, date: LocalDate, unit: CountUnit): Milestone? {
        if (!date.isAfter(start)) return null
        val totalDays = ChronoUnit.DAYS.between(start, date)
        val totalMonths = completedMonths(start, date)
        val isMonthDay = totalMonths > 0 && start.plusMonths(totalMonths) == date

        // Year anniversaries are always celebrated
        if (isMonthDay && totalMonths % 12 == 0L) return Milestone(totalMonths / 12, CountUnit.YEARS)
        // Every 100 days is always celebrated
        if (totalDays % 100 == 0L) return Milestone(totalDays, CountUnit.DAYS)
        // Monthly ones, unless the counter is set to years only
        if (isMonthDay && unit != CountUnit.YEARS) return Milestone(totalMonths, CountUnit.MONTHS)
        // Weekly ones, only when counting in weeks
        if (unit == CountUnit.WEEKS && totalDays % 7 == 0L) return Milestone(totalDays / 7, CountUnit.WEEKS)
        return null
    }

    /**
     * Turns a milestone into the celebration text, using the custom message if there is one.
     * "Happy {n} {unit}, babe! ❤️" + 2 months -> "Happy 2 months, babe! ❤️"
     */
    fun celebrationText(counter: Counter, milestone: Milestone): String {
        val template = counter.customMessage.trim()
        if (template.isEmpty()) return "Happy ${milestone.label}!"
        val unitWord = if (milestone.count == 1L) milestone.unit.singular else milestone.unit.plural
        return template
            .replace("{n}", milestone.count.toString())
            .replace("{unit}", unitWord)
            .replace("{title}", counter.title)
    }

    /** The celebration text for [date], or null if it's not a milestone day. */
    fun celebrationOn(counter: Counter, date: LocalDate): String? =
        milestoneOn(counter.startDate, date, counter.unit)?.let { celebrationText(counter, it) }

    /**
     * The next [count] milestones after [today]. Walks forward day by day using the same
     * rules as the celebrations, so the list always matches what the app will celebrate.
     */
    fun upcoming(counter: Counter, today: LocalDate, count: Int = 10): List<UpcomingMilestone> {
        val result = mutableListOf<UpcomingMilestone>()
        var date = if (today.isBefore(counter.startDate)) counter.startDate else today
        val limit = date.plusYears(5)
        while (result.size < count && date.isBefore(limit)) {
            date = date.plusDays(1)
            milestoneOn(counter.startDate, date, counter.unit)?.let {
                result += UpcomingMilestone(date, it, ChronoUnit.DAYS.between(today, date))
            }
        }
        return result
    }

    fun display(counter: Counter, today: LocalDate = LocalDate.now()): CounterDisplay {
        val start = counter.startDate
        val unit = counter.unit
        val modeLabel = when (counter.mode) {
            CountMode.LIVE -> "Live"
            CountMode.MILESTONE -> "Updates every ${unit.singular}"
        }

        // Start date is in the future: count down to it
        if (today.isBefore(start)) {
            val daysLeft = ChronoUnit.DAYS.between(today, start)
            return CounterDisplay(
                headline = "In ${plural(daysLeft, CountUnit.DAYS)}",
                detail = "Starts ${formatDate(start)}",
                celebration = null,
                nextMilestone = null,
                nextShort = null,
                modeLabel = modeLabel
            )
        }

        if (today == start) {
            val (next, nextShort) = nextText(start, today, unit)
            return CounterDisplay(
                headline = "Day 1",
                detail = "Started ${formatDate(start)}",
                celebration = "Today's the day!",
                nextMilestone = next,
                nextShort = nextShort,
                modeLabel = modeLabel
            )
        }

        val totalDays = ChronoUnit.DAYS.between(start, today)
        val headline: String
        val detail: String
        when (counter.mode) {
            CountMode.LIVE -> {
                if (unit == CountUnit.DAYS) {
                    headline = plural(totalDays, CountUnit.DAYS)
                    detail = breakdown(start, today, CountUnit.YEARS)
                } else {
                    headline = breakdown(start, today, unit)
                    detail = "${plural(totalDays, CountUnit.DAYS)} total"
                }
            }
            CountMode.MILESTONE -> {
                val n = completedUnits(start, today, unit)
                headline = if (n == 0L) "Under 1 ${unit.singular}" else plural(n, unit)
                detail = "Since ${formatDate(start)}"
            }
        }

        val (next, nextShort) = nextText(start, today, unit)
        return CounterDisplay(
            headline = headline,
            detail = detail,
            celebration = celebrationOn(counter, today),
            nextMilestone = next,
            nextShort = nextShort,
            modeLabel = modeLabel
        )
    }

    /** Next milestone. For DAYS counters the next milestone is the next multiple of 100 days. */
    private fun nextText(start: LocalDate, today: LocalDate, unit: CountUnit): Pair<String, String> {
        val nextDate: LocalDate
        val label: String
        if (unit == CountUnit.DAYS) {
            val totalDays = ChronoUnit.DAYS.between(start, today)
            val nextHundred = (totalDays / 100 + 1) * 100
            nextDate = start.plusDays(nextHundred)
            label = plural(nextHundred, CountUnit.DAYS)
        } else {
            val n = completedUnits(start, today, unit) + 1
            nextDate = milestoneDate(start, unit, n)
            label = plural(n, unit)
        }
        val daysUntil = ChronoUnit.DAYS.between(today, nextDate)
        val inText = if (daysUntil == 1L) "tomorrow" else "in $daysUntil days"
        return "Next: $label on ${formatDate(nextDate)} ($inText)" to "$label $inText"
    }
}