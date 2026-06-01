package com.example.receiver

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * BroadcastReceiver для перехвата сигналов будильника от AlarmManager.
 * При срабатывании будильника выводит push-уведомление пользователю.
 */
class IntakeAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val logId = intent.getLongExtra("INTAKE_LOG_ID", 0L)
        val medicationName = intent.getStringExtra("MEDICATION_NAME") ?: "Лекарство"
        val dosage = intent.getStringExtra("DOSAGE") ?: ""

        Log.d("IntakeAlarmReceiver", "Получен сигнал будильника для лога $logId ($medicationName, $dosage)")

        showNotification(context, logId, medicationName, dosage)
    }

    /**
     * Создает канал уведомлений и отправляет Push-уведомление в систему.
     */
    private fun showNotification(context: Context, logId: Long, medicationName: String, dosage: String) {
        val channelId = "smart_pills_notifications"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Создание канала для Android 8.0+ (Oreo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Напоминания о приеме лекарств",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал для важных уведомлений о приеме медикаментов SmartPills"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Клик по уведомлению открывает приложение (MainActivity)
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            logId.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Построение уведомления
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Системная иконка, 100% безопасная от крашей
            .setContentTitle("SmartPills: Время приема!")
            .setContentText("Пожалуйста, примите $medicationName ($dosage)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Пора принять лекарство: $medicationName ($dosage).\nНе забудьте отметить прием в приложении SmartPills!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Уникальный ID уведомления — хэш-код идентификатора записи
        notificationManager.notify(logId.hashCode(), notification)
    }
}

/**
 * Класс-менеджер для планирования уведомлений в AlarmManager.
 * Использует setExactAndAllowWhileIdle, чтобы сработать точно по расписанию, в том числе в режиме энергосбережения (Doze Mode).
 */
object AlarmScheduler {

    /**
     * Планирование будильника для конкретной записи приема
     */
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarm(
        context: Context,
        logId: Long,
        medicationName: String,
        dosage: String,
        dateStr: String, // Формат "YYYY-MM-DD"
        timeStr: String  // Формат "HH:MM"
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Формирование точного штампа времени
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val dateTimeString = "$dateStr $timeStr"
        val calendar = Calendar.getInstance()
        try {
            val date = sdf.parse(dateTimeString) ?: return
            calendar.time = date
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Ошибка парсинга даты: $dateTimeString", e)
            return
        }

        // Если запланированное время уже в прошлом — аларм не ставим
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            Log.d("AlarmScheduler", "Пропуск планирования: время $dateTimeString уже прошло.")
            return
        }

        val intent = Intent(context, IntakeAlarmReceiver::class.java).apply {
            putExtra("INTAKE_LOG_ID", logId)
            putExtra("MEDICATION_NAME", medicationName)
            putExtra("DOSAGE", dosage)
        }

        // Уникальный PendingIntent по хэш-коду logId
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            logId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Планирование точного аларма
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }

        Log.d("AlarmScheduler", "Аларм запланирован для лога $logId ($medicationName) на $dateTimeString")
    }

    /**
     * Отмена запланированного будильника
     */
    fun cancelAlarm(context: Context, logId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, IntakeAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            logId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            Log.d("AlarmScheduler", "Аларм для лога $logId отменен.")
        }
    }
}
