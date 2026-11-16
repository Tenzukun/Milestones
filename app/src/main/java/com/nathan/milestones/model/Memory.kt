package com.nathan.milestones.model

import java.time.LocalDate

/** A note (and optional photo) saved for a date, e.g. "1 month: sushi + movie night". */
data class Memory(
    val id: String,
    /** Which date (Counter) this memory belongs to */
    val counterId: String,
    val date: LocalDate,
    val title: String,
    val note: String = "",
    /** Full path to the photo copied into the app's private storage, or null */
    val photoPath: String? = null
)