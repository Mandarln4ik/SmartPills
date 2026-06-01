package com.example.data

import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий (Repository) для абстрагирования источника данных.
 * Согласно паттерну Clean Architecture, ViewModel не общается с DAO напрямую,
 * а делает запросы через репозиторий.
 */
class PillsRepository(private val pillsDao: PillsDao) {

    /**
     * Сохранить лекарство и получить его сгенерированный ID
     */
    suspend fun insertMedication(medication: Medication): Long {
        return pillsDao.insertMedication(medication)
    }

    /**
     * Сохранить список логов приема
     */
    suspend fun insertIntakeLogs(logs: List<IntakeLog>) {
        pillsDao.insertIntakeLogs(logs)
    }

    /**
     * Изменить статус приема лекарства
     */
    suspend fun updateIntakeLogStatus(id: Long, isTaken: Boolean) {
        pillsDao.updateIntakeLogStatus(id, isTaken)
    }

    /**
     * Поток логов приемов лекарств на указанную дату
     */
    fun getIntakesWithMedicationForDate(actualDate: String): Flow<List<IntakeWithMedication>> {
        return pillsDao.getIntakesWithMedicationForDate(actualDate)
    }

    /**
     * Получить логи приемов в заданном интервале дат (для недельной статистики)
     */
    fun getIntakeLogsBetweenDates(startDate: String, endDate: String): Flow<List<IntakeLog>> {
        return pillsDao.getIntakeLogsBetweenDates(startDate, endDate)
    }

    /**
     * Получить запланированные будущие приемы для восстановления алармов в AlarmManager
     */
    suspend fun getUpcomingIntakes(todayDate: String): List<IntakeWithMedication> {
        return pillsDao.getUpcomingIntakes(todayDate)
    }
}
