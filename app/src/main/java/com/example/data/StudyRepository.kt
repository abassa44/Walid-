package com.example.data

import kotlinx.coroutines.flow.Flow

class StudyRepository(private val studyDao: StudyDao) {
    val allTasks: Flow<List<StudyTask>> = studyDao.getAllTasks()
    val allAppointments: Flow<List<StudyAppointment>> = studyDao.getAllAppointments()
    val allSessions: Flow<List<StudySession>> = studyDao.getAllSessions()

    suspend fun insertTask(task: StudyTask) = studyDao.insertTask(task)
    suspend fun updateTask(task: StudyTask) = studyDao.updateTask(task)
    suspend fun deleteTask(task: StudyTask) = studyDao.deleteTask(task)

    suspend fun insertAppointment(appointment: StudyAppointment) = studyDao.insertAppointment(appointment)
    suspend fun updateAppointment(appointment: StudyAppointment) = studyDao.updateAppointment(appointment)
    suspend fun deleteAppointment(appointment: StudyAppointment) = studyDao.deleteAppointment(appointment)

    suspend fun insertSession(session: StudySession) = studyDao.insertSession(session)
    suspend fun updateSession(session: StudySession) = studyDao.updateSession(session)
    suspend fun deleteSession(session: StudySession) = studyDao.deleteSession(session)
}
