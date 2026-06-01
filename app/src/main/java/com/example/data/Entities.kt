package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Сущность лекарственного препарата (Таблица Medication).
 * Хранит общую информацию о назначенном курсе приема лекарства.
 */
@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,               // Уникальный идентификатор (автогенерация)
    val name: String,               // Название лекарства (например, "Ибупрофен")
    val dosage: String,             // Дозировка (например, "1 таблетка", "400 мг")
    val startDate: String,          // Дата начала курса в формате "YYYY-MM-DD"
    val endDate: String,            // Дата окончания курса в формате "YYYY-MM-DD"
    val timesPerDay: Int,            // Количество приемов в день
    val iconName: String = "Medication", // Имя выбранной иконки ("Medication", "Vaccines", "Healing", "WaterDrop", "Favorite", "Thermostat")
    val iconColor: String = "Blue"  // Кодировка цвета иконки ("Blue", "Red", "Green", "Orange", "Purple", "Pink")
)

/**
 * Сущность лога приема (Таблица IntakeLog).
 * Связана отношением "Один-ко-многим" к Medication.
 * Содержит конкретные запланированные события приема.
 */
@Entity(
    tableName = "intake_logs",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE // При удалении лекарства удаляются все его логи приема
        )
    ],
    indices = [Index(value = ["medicationId"]), Index(value = ["actualDate"])]
)
data class IntakeLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,               // Уникальный идентификатор лога
    val medicationId: Long,         // Ссылка на ID лекарства
    val scheduledTime: String,      // Запланированное время приема в формате "HH:MM"
    val actualDate: String,         // Конкретная дата запланированного приема в формате "YYYY-MM-DD"
    val isTaken: Boolean = false    // Отметка о том, принято ли лекарство
)

/**
 * Вспомогательный класс для объединения IntakeLog с Medication.
 * Позволяет получить лог вместе с полной информацией о лекарстве одним запросом в БД.
 */
data class IntakeWithMedication(
    @Embedded val intakeLog: IntakeLog,
    @Relation(
        parentColumn = "medicationId",
        entityColumn = "id"
    )
    val medication: Medication
)
