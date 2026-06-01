package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.IntakeLog
import com.example.data.IntakeWithMedication
import com.example.data.Medication
import com.example.data.PillsDatabase
import com.example.data.PillsRepository
import com.example.receiver.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Модель представления (ViewModel) для управления состоянием UI и бизнес-логикой.
 * Реализует паттерн MVVM. Наследуется от AndroidViewModel для доступа к контексту приложения.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PillsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PillsRepository

    // Текущая выбранная дата в формате "YYYY-MM-DD"
    private val _selectedDate = MutableStateFlow(getCurrentDateStr())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    init {
        val database = PillsDatabase.getDatabase(application)
        repository = PillsRepository(database.pillsDao())
    }

    /**
     * Поток логов приемов лекарств на выбранную дату.
     * Автоматически перезапускается и обновляет UI при смене выбранной даты.
     */
    val todayIntakes: StateFlow<List<IntakeWithMedication>> = _selectedDate
        .flatMapLatest { date ->
            repository.getIntakesWithMedicationForDate(date)
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Статистика за текущую неделю (количество приемов и процент выполнения)
     */
    data class WeeklyStats(
        val totalScheduled: Int = 0,
        val totalTaken: Int = 0,
        val percentage: Float = 0f
    )

    // Текущий интервал дат недели: Пара (Дата начала, Дата окончания)
    private val weekRange: Pair<String, String> = getCurrentWeekDateRange()

    /**
     * Поток данных недельной статистики.
     * Пересчитывается реактивно при любом изменении в базе данных.
     */
    val weeklyStats: StateFlow<WeeklyStats> = repository.getIntakeLogsBetweenDates(weekRange.first, weekRange.second)
        .map { logs ->
            val total = logs.size
            val taken = logs.count { it.isTaken }
            val pct = if (total > 0) (taken.toFloat() / total.toFloat()) else 0f
            WeeklyStats(totalScheduled = total, totalTaken = taken, percentage = pct)
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WeeklyStats()
        )

    /**
     * Изменение выбранной пользователем даты в календаре
     */
    fun selectDate(dateStr: String) {
        _selectedDate.value = dateStr
    }

    /**
     * Переключение статуса приема (isTaken) лекарства.
     * Мгновенно обновляет статус в БД.
     */
    fun toggleIntakeStatus(intake: IntakeWithMedication) {
        viewModelScope.launch(Dispatchers.IO) {
            val log = intake.intakeLog
            val med = intake.medication
            val newStatus = !log.isTaken

            // Обновляем значение в БД
            repository.updateIntakeLogStatus(log.id, newStatus)

            // Умная логика управления алармами:
            if (newStatus) {
                // Если таблетка принята, отменяем предстоящее уведомление для неё
                AlarmScheduler.cancelAlarm(getApplication(), log.id)
            } else {
                // Если отметка снята, и время еще не пришло — планируем аларм заново
                AlarmScheduler.scheduleAlarm(
                    context = getApplication(),
                    logId = log.id,
                    medicationName = med.name,
                    dosage = med.dosage,
                    dateStr = log.actualDate,
                    timeStr = log.scheduledTime
                )
            }
        }
    }

    /**
     * Бизнес-логика добавления курса лекарства.
     * 1. Сохраняет описание Medication в БД.
     * 2. Извлекает ID.
     * 3. Генерирует в цикле записи IntakeLog для всех дней от startDate до endDate.
     * 4. Планирует точные будильники в AlarmManager для будущих приёмов.
     */
    fun addMedicationAndGenerateSchedule(
        name: String,
        dosage: String,
        startDateStr: String,
        endDateStr: String,
        timesPerDay: Int,
        iconName: String = "Medication",
        iconColor: String = "Blue"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Сохранение лекарства в БД
                val medication = Medication(
                    name = name,
                    dosage = dosage,
                    startDate = startDateStr,
                    endDate = endDateStr,
                    timesPerDay = timesPerDay,
                    iconName = iconName,
                    iconColor = iconColor
                )
                val medicationId = repository.insertMedication(medication)

                // 2. Генерация списка дат в курсе лечения
                val datesList = getDatesListBetween(startDateStr, endDateStr)
                // Определение временных точек в сутках на основе частоты
                val timesList = getScheduledTimesForFrequency(timesPerDay)

                val logsToInsert = mutableListOf<IntakeLog>()

                // Генерируем лог приемов для каждого дня и времени
                for (date in datesList) {
                    for (time in timesList) {
                        logsToInsert.add(
                            IntakeLog(
                                medicationId = medicationId,
                                scheduledTime = time,
                                actualDate = date,
                                isTaken = false
                            )
                        )
                    }
                }

                // Вставка сгенерированных записей в БД
                repository.insertIntakeLogs(logsToInsert)

                // 3. Планирование будильников для всех только что созданных записей приема лекарств в будущем.
                // Чтобы получить реальные сгенерированные ID записей IntakeLog (после вставки в БД),
                // сделаем быстрый запрос из БД этих только что вставленных логов.
                val todayDateStr = getCurrentDateStr()
                val createdIntakes = repository.getUpcomingIntakes(todayDateStr)
                    .filter { it.intakeLog.medicationId == medicationId }

                for (intake in createdIntakes) {
                    AlarmScheduler.scheduleAlarm(
                        context = getApplication(),
                        logId = intake.intakeLog.id,
                        medicationName = name,
                        dosage = dosage,
                        dateStr = intake.intakeLog.actualDate,
                        timeStr = intake.intakeLog.scheduledTime
                    )
                }

                Log.d("PillsViewModel", "Успешно сгенерирован курс лечения для $name. Логов: ${logsToInsert.size}")

            } catch (e: Exception) {
                Log.e("PillsViewModel", "Ошибка при генерации расписания лекарства", e)
            }
        }
    }

    // --- Вспомогательные утилитарные методы ---

    /**
     * Получить текущую дату в текстовом формате API "YYYY-MM-DD"
     */
    fun getCurrentDateStr(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    /**
     * Получить список дат "YYYY-MM-DD" в интервале от startDate до endDate
     */
    private fun getDatesListBetween(startDate: String, endDate: String): List<String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val datesList = mutableListOf<String>()
        try {
            val startCal = Calendar.getInstance().apply {
                time = sdf.parse(startDate) ?: Date()
            }
            val endCal = Calendar.getInstance().apply {
                time = sdf.parse(endDate) ?: Date()
            }

            while (!startCal.after(endCal)) {
                datesList.add(sdf.format(startCal.time))
                startCal.add(Calendar.DATE, 1)
            }
        } catch (e: Exception) {
            Log.e("PillsViewModel", "Ошибка генерации списка дат", e)
        }
        return datesList
    }

    /**
     * Генерирует массив времен приемов ("HH:MM") в зависимости от количества раз в день (timesPerDay).
     */
    private fun getScheduledTimesForFrequency(timesPerDay: Int): List<String> {
        return when (timesPerDay) {
            1 -> listOf("09:00")
            2 -> listOf("09:00", "21:00")
            3 -> listOf("09:00", "15:00", "21:00")
            4 -> listOf("08:00", "12:00", "16:00", "20:00")
            else -> {
                // Если приемов больше 4, делим день с 08:00 до 22:00 (14 часов) на равные интервалы
                val times = mutableListOf<String>()
                val startHour = 8
                val endHour = 22
                val totalMinutes = (endHour - startHour) * 60
                val interval = if (timesPerDay > 1) totalMinutes / (timesPerDay - 1) else 0
                for (i in 0 until timesPerDay) {
                    val currentMins = startHour * 60 + i * interval
                    val h = currentMins / 60
                    val m = currentMins % 60
                    times.add(String.format(Locale.US, "%02d:%02d", h, m))
                }
                times
            }
        }
    }

    /**
     * Возвращает диапазон дат (начало и конец) текущей недели (с понедельника по воскресенье).
     */
    private fun getCurrentWeekDateRange(): Pair<String, String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startStr = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_WEEK, 6)
        val endStr = sdf.format(cal.time)

        return Pair(startStr, endStr)
    }
}
