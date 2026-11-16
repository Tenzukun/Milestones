package com.nathan.milestones.model

/** One colour choice: a gradient from [start] to [end] (written as 0xAARRGGBB). */
data class CounterColor(val name: String, val start: Long, val end: Long)

object CounterColors {
    val all = listOf(
        CounterColor("Pink", 0xFFE0457B, 0xFFFF9671),
        CounterColor("Red", 0xFFD32F2F, 0xFFFF7961),
        CounterColor("Orange", 0xFFF06A2D, 0xFFFFC94A),
        CounterColor("Green", 0xFF1E9E6A, 0xFF7ED957),
        CounterColor("Teal", 0xFF00897B, 0xFF4DD0E1),
        CounterColor("Blue", 0xFF2F6FE0, 0xFF4FC3FF),
        CounterColor("Purple", 0xFF7B4AE0, 0xFFC471FF),
        CounterColor("Night", 0xFF2C2C54, 0xFF5C5C8A)
    )

    /** Safe lookup: an unknown number falls back to Pink. */
    fun get(index: Int): CounterColor = all.getOrElse(index) { all[0] }
}

/** Quick-pick emojis on the edit screen. You can still type any emoji. */
val EMOJI_CHOICES = listOf("❤️", "💕", "💍", "🎂", "🎉", "⭐", "🌸", "🐶", "🏋️", "✈️")