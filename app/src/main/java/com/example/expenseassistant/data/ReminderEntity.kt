package com.example.expenseassistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val hourOfDay: Int,
    val minute: Int,
    val isRecurringDaily: Boolean = true,
    val isActive: Boolean = true,
    val categoryHint: String? = null,
    val reason: String = "User requested",
    val createdAt: Long = System.currentTimeMillis()
)
