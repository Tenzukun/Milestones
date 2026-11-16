package com.nathan.milestones.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nathan.milestones.model.Counter
import com.nathan.milestones.model.CounterColor
import com.nathan.milestones.model.CounterColors

/** The gradient for a colour choice, e.g. pink -> peach. */
fun CounterColor.brush(): Brush = Brush.linearGradient(listOf(Color(start), Color(end)))

/** The gradient for a date, e.g. counter.brush() */
fun Counter.brush(): Brush = CounterColors.get(colorIndex).brush()