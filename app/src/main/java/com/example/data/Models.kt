package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_tasks")
data class StudyTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subject: String,
    val dueDate: Long,
    val isCompleted: Boolean = false,
    val priority: String = "Medium", // High, Medium, Low
    val notes: String = ""
)

@Entity(tableName = "study_appointments")
data class StudyAppointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subject: String,
    val dateTime: Long,
    val durationMinutes: Int = 60,
    val location: String = "",
    val notes: String = ""
)

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val date: Long,
    val durationMinutes: Int,
    val completedMinutes: Int = 0,
    val isCompleted: Boolean = false,
    val focusScore: Int = 0, // 1-5 rating
    val notes: String = ""
)
