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

    /** The gradient for a date: its custom colour if it has one, otherwise its preset. */
    fun forCounter(counter: Counter): CounterColor =
        counter.customColor?.let { fromCustom(it) } ?: get(counter.colorIndex)

    /**
     * Turns one picked colour into a gradient: the end colour is the same colour
     * shifted a little around the colour wheel and made a bit lighter.
     */
    fun fromCustom(color: Long): CounterColor {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toInt(), hsv)
        val end = floatArrayOf(
            (hsv[0] + 25f) % 360f,                // hue: shift around the wheel
            (hsv[1] * 0.8f).coerceIn(0f, 1f),     // saturation: a bit softer
            (hsv[2] + 0.15f).coerceAtMost(1f)     // brightness: a bit lighter
        )
        val endColor = android.graphics.Color.HSVToColor(end).toLong() and 0xFFFFFFFFL
        return CounterColor("Custom", color, endColor)
    }

    /** Builds a colour (0xAARRGGBB) from hue 0-360, saturation 0-1, brightness 0-1. */
    fun fromHsv(hue: Float, saturation: Float, brightness: Float): Long =
        android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)).toLong() and 0xFFFFFFFFL
}

/** Quick-pick emojis on the edit screen. You can still type any emoji. */
val EMOJI_CHOICES = listOf("❤️", "💕", "💍", "🎂", "🎉", "⭐", "🌸", "🐶", "🏋️", "✈️")