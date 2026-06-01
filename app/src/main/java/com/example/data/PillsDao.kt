package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс доступа к данным (DAO) приложения SmartPills.
 * Содержит SQL-запросы для выполнения операций CRUD в Room БД.
 */
@Dao
interface PillsDao {

    /**
     * Вставка нового лекарства. Возвращает сгенерированный ID,
     * который используется для привязки логов приема.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    /**
     * Вставка списка логов приемов лекарства (коллекция IntakeLog).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntakeLogs(logs: List<IntakeLog>)

    /**
     * Обновление статуса приема лекарства (isTaken = true/false) для конкретного лога.
     */
    @Query("UPDATE intake_logs SET isTaken = :isTaken WHERE id = :id")
    suspend fun updateIntakeLogStatus(id: Long, isTaken: Boolean)

    /**
     * Получить все логи приемов на конкретную дату (actualDate) с деталями Medikement.
     * Возвращает поток данных Flow для автоматического обновления UI при изменении в БД.
     */
    @Transaction
    @Query("SELECT * FROM intake_logs WHERE actualDate = :actualDate ORDER BY scheduledTime ASC")
    fun getIntakesWithMedicationForDate(actualDate: String): Flow<List<IntakeWithMedication>>

    /**
     * Получить логи приемов в заданном интервале дат (для расчета недельной статистики).
     */
    @Query("SELECT * FROM intake_logs WHERE actualDate BETWEEN :startDate AND :endDate")
    fun getIntakeLogsBetweenDates(startDate: String, endDate: String): Flow<List<IntakeLog>>

    /**
     * Получить все будущие (невыполненные) логи приемов лекарств (используется для перепланирования алармов при загрузке устройства).
     */
    @Transaction
    @Query("SELECT * FROM intake_logs WHERE actualDate >= :todayDate AND isTaken = 0")
    suspend fun getUpcomingIntakes(todayDate: String): List<IntakeWithMedication>
}
