package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.PillsDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BroadcastReceiver для перехвата системного события окончания загрузки ОС (BOOT_COMPLETED).
 * Восстанавливает все запланированные будильники из базы данных в AlarmManager.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Система загружена (Boot Completed). Запуск перепланирования алармов...")

            val pendingResult = goAsync() // Сигнализирует системе, что нужно держать процесс активным во время асинхронного потока
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = PillsDatabase.getDatabase(context)
                    val dao = db.pillsDao()
                    
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    // Получаем все планируемые приемы, начиная с сегодняшнего дня, которые еще не были приняты
                    val upcomingLogs = dao.getUpcomingIntakes(todayStr)
                    
                    Log.d("BootReceiver", "Найдено предстоящих невыполненных приемов: ${upcomingLogs.size}")
                    
                    for (intake in upcomingLogs) {
                        AlarmScheduler.scheduleAlarm(
                            context = context,
                            logId = intake.intakeLog.id,
                            medicationName = intake.medication.name,
                            dosage = intake.medication.dosage,
                            dateStr = intake.intakeLog.actualDate,
                            timeStr = intake.intakeLog.scheduledTime
                        )
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Ошибка при восстановлении будильников после перезагрузки", e)
                } finally {
                    pendingResult?.finish() // Освобождаем процесс
                }
            }
        }
    }
}
