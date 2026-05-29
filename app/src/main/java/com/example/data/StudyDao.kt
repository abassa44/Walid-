package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    // --- Study Tasks Queries ---
    @Query("SELECT * FROM study_tasks ORDER BY isCompleted ASC, dueDate ASC")
    fun getAllTasks(): Flow<List<StudyTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTask)

    @Update
    suspend fun updateTask(task: StudyTask)

    @Delete
    suspend fun deleteTask(task: StudyTask)

    // --- Study Appointments Queries ---
    @Query("SELECT * FROM study_appointments ORDER BY dateTime ASC")
    fun getAllAppointments(): Flow<List<StudyAppointment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: StudyAppointment)

    @Update
    suspend fun updateAppointment(appointment: StudyAppointment)

    @Delete
    suspend fun deleteAppointment(appointment: StudyAppointment)

    // --- Study Sessions Queries ---
    @Query("SELECT * FROM study_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession)

    @Update
    suspend fun updateSession(session: StudySession)

    @Delete
    suspend fun deleteSession(session: StudySession)
}
