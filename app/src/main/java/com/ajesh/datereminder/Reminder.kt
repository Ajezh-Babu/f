package com.ajesh.datereminder

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Reminder(
    val id: Long,
    val title: String,
    val timeMillis: Long,
    val done: Boolean = false
) {
    val date: LocalDate
        get() = Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
}
