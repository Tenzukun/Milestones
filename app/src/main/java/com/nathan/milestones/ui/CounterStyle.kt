package com.nathan.milestones.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nathan.milestones.model.Counter
import com.nathan.milestones.model.CounterColor
import com.nathan.milestones.model.CounterColors

/** The gradient for a colour choice, e.g. pink -> peach. */
fun CounterColor.brush(): Brush = Brush.linearGradient(listOf(Color(start), Color(end)))

/** The gradient for a date (custom colour if set, otherwise its preset). */
fun Counter.brush(): Brush = CounterColors.forCounter(this).brush()

/** A rainbow, used for the "pick your own colour" circle. */
val RainbowBrush: Brush = Brush.sweepGradient(
    listOf(
        Color(0xFFFF4D4D), Color(0xFFFFC94A), Color(0xFF7ED957),
        Color(0xFF4DD0E1), Color(0xFF4F7BFF), Color(0xFFC471FF), Color(0xFFFF4D4D)
    )
)