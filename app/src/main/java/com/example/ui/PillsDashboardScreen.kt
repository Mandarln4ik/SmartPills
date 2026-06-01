package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IntakeWithMedication
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Получить векторную иконку на основе имени, сохраненного в БД.
 */
fun getIconForName(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (name) {
        "Favorite" -> Icons.Default.Favorite
        "Star" -> Icons.Default.Star
        "Info" -> Icons.Default.Info
        "Warning" -> Icons.Default.Warning
        else -> Icons.Default.Medication
    }
}

/**
 * Получить цвет на основе названия цвета, сохраненного в БД.
 */
fun getColorForName(name: String): androidx.compose.ui.graphics.Color {
    return when (name) {
        "Blue" -> androidx.compose.ui.graphics.Color(0xFF2196F3)
        "Red" -> androidx.compose.ui.graphics.Color(0xFFE53935)
        "Green" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        "Orange" -> androidx.compose.ui.graphics.Color(0xFFFF9800)
        "Purple" -> androidx.compose.ui.graphics.Color(0xFF9C27B0)
        "Pink" -> androidx.compose.ui.graphics.Color(0xFFE91E63)
        else -> androidx.compose.ui.graphics.Color(0xFF2196F3)
    }
}

/**
 * Получить название фазы дня на основе времени приема лекарства.
 */
fun getPhaseDescription(timeStr: String): String {
    val hour = timeStr.substringBefore(":").toIntOrNull() ?: 12
    return when (hour) {
        in 5..11 -> "Утро"
        in 12..16 -> "День"
        in 17..21 -> "Вечер"
        else -> "Ночь"
    }
}

/**
 * Получить соответствующий эмодзи для фазы дня.
 */
fun getPhaseIcon(phase: String): String {
    return when (phase) {
        "Утро" -> "☀️"
        "День" -> "🌤️"
        "Вечер" -> "🌅"
        else -> "🌙"
    }
}

/**
 * Получить числовой приоритет для сортировки фар ко дню.
 */
fun getPhaseOrder(phase: String): Int {
    return when (phase) {
        "Утро" -> 1
        "День" -> 2
        "Вечер" -> 3
        else -> 4
    }
}

/**
 * Главный экран SmartPills.
 * Отображает интерактивный календарь с удобной пагинацией под любые даты,
 * компактный расчет прогресса за выбранный день и форму добавления новых курсов.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PillsDashboardScreen(
    viewModel: PillsViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val intakes by viewModel.todayIntakes.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showCalendarDatePicker by remember { mutableStateOf(false) }
    var todayClickTrigger by remember { mutableStateOf(0) }

    // Конвертация даты в красивый русский формат для заголовка календаря (например, "Июнь 2026")
    val displayMonth = remember(selectedDate) {
        try {
            val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsedDate = dbFormat.parse(selectedDate) ?: Date()
            val formatRu = SimpleDateFormat("LLLL yyyy", Locale("ru"))
            formatRu.format(parsedDate).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            "Календарь"
        }
    }

    // Полный текст даты для детального отображения
    val displayDate = remember(selectedDate) {
        try {
            val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsedDate = dbFormat.parse(selectedDate) ?: Date()
            val formatRu = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))
            formatRu.format(parsedDate).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            selectedDate
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .testTag("add_medication_fab")
                    .padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить лекарство",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Компактный заголовок приложения
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SmartPills",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("app_logo_title")
                    )
                    Text(
                        text = "Умное расписание и контроль приемов",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                // Кнопка быстрого сброса выбранной даты на "Сегодня"
                IconButton(
                    onClick = {
                        viewModel.selectDate(viewModel.getCurrentDateStr())
                        todayClickTrigger++
                    },
                    modifier = Modifier
                        .testTag("calendar_today_button")
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Сегодня",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Интерактивный календарь с листалкой и быстрой навигацией
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_navigation_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Панель навигации по месяцам/дням
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                cal.time = sdf.parse(selectedDate) ?: Date()
                                cal.add(Calendar.DATE, -1)
                                viewModel.selectDate(sdf.format(cal.time))
                            },
                            modifier = Modifier.testTag("prev_day_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Предыдущий день",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Текст Месяца с красивой плашкой. Клик открывает пикер!
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showCalendarDatePicker = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Календарь",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = displayMonth,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                cal.time = sdf.parse(selectedDate) ?: Date()
                                cal.add(Calendar.DATE, 1)
                                viewModel.selectDate(sdf.format(cal.time))
                            },
                            modifier = Modifier.testTag("next_day_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Следующий день",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Линейка дней недели свайпами прокручиваемая
                    SwipeableDaySelectorRow(
                        selectedDate = selectedDate,
                        todayClickTrigger = todayClickTrigger,
                        onDateSelected = { viewModel.selectDate(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Расчет и вывод статистики приемов за конкретный выбранный день
            val totalToday = intakes.size
            val takenToday = intakes.count { it.intakeLog.isTaken }
            val dailyProgress = if (totalToday > 0) takenToday.toFloat() / totalToday.toFloat() else 0f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedDate == viewModel.getCurrentDateStr()) "Сегодня: $displayDate" else displayDate,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Прогресс приема
                Text(
                    text = "Принято: $takenToday из $totalToday",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dailyProgress == 1f && totalToday > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                )
            }

            if (totalToday > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = dailyProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("daily_progress_bar"),
                    color = if (dailyProgress == 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Список логов приемов лекарств
            if (intakes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = "Расписание пусто",
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "На этот день нет назначений.\nНажмите '+' внизу, чтобы добавить первое лекарство.",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                val groupedIntakes = remember(intakes) {
                    intakes.groupBy { getPhaseDescription(it.intakeLog.scheduledTime) }
                        .toList()
                        .sortedBy { getPhaseOrder(it.first) }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("intake_log_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedIntakes.forEach { (phase, list) ->
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = getPhaseIcon(phase),
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = phase,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                )
                            }
                        }
                        items(list, key = { it.intakeLog.id }) { intake ->
                            IntakeCard(
                                intake = intake,
                                onToggleStatus = { viewModel.toggleIntakeStatus(intake) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("intake_card_${intake.intakeLog.id}")
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // отвод под FAB
                    }
                }
            }
        }
    }

    // Календарь-диалог выбора ЛЮБОЙ даты для удобного листания
    if (showCalendarDatePicker) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val initialMillis = remember(selectedDate) {
            try {
                sdf.parse(selectedDate)?.time
            } catch (e: Exception) {
                null
            }
        }
        M3DatePickerDialog(
            initialDateMillis = initialMillis,
            onDismiss = { showCalendarDatePicker = false },
            onDateSelected = { timeMillis ->
                if (timeMillis != null) {
                    val pickedSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    pickedSdf.timeZone = TimeZone.getTimeZone("UTC")
                    val dateFormatted = pickedSdf.format(Date(timeMillis))
                    viewModel.selectDate(dateFormatted)
                }
                showCalendarDatePicker = false
            }
        )
    }

    // Диалог добавления лекарства
    if (showAddDialog) {
        AddMedicationDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, dosage, start, end, times, iconName, iconColor ->
                viewModel.addMedicationAndGenerateSchedule(name, dosage, start, end, times, iconName, iconColor)
                showAddDialog = false
            }
        )
    }
}

/**
 * Вспомогательный диалог выбора даты на базе Material 3 DatePicker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3DatePickerDialog(
    initialDateMillis: Long?,
    onDismiss: () -> Unit,
    onDateSelected: (Long?) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis ?: System.currentTimeMillis()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
            }) {
                Text("ОК")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

/**
 * Карточка приёма конкретного лекарства (Бизнес-задача №4).
 * Меняет цвет на легкий зелёный градиент, если таблетка принята.
 */
@Composable
fun IntakeCard(
    intake: IntakeWithMedication,
    onToggleStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTaken = intake.intakeLog.isTaken
    val med = intake.medication
    val log = intake.intakeLog

    val containerColor by animateColorAsState(
        targetValue = if (isTaken) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(),
        label = "cardColor"
    )

    val elevation = if (isTaken) {
        CardDefaults.cardElevation(defaultElevation = 2.dp)
    } else {
        CardDefaults.cardElevation(defaultElevation = 1.dp)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Иконка
                val iconColorValue = remember(med.iconColor) { getColorForName(med.iconColor) }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isTaken) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            else iconColorValue.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconForName(med.iconName),
                        contentDescription = "Лекарство",
                        tint = if (isTaken) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else iconColorValue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = med.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isTaken) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isTaken) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Время",
                            tint = if (isTaken) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = log.scheduledTime,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isTaken) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textDecoration = if (isTaken) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Доза: ${med.dosage}",
                            fontSize = 13.sp,
                            color = if (isTaken) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textDecoration = if (isTaken) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }
            }

            // Кастомный чекбокс (Доступность 48dp)
            CustomCheckbox(
                checked = isTaken,
                onCheckedChange = { onToggleStatus() },
                modifier = Modifier.testTag("checkbox_${log.id}")
            )
        }
    }
}

@Composable
fun CustomCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Box(
            modifier = modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable { onCheckedChange(!checked) },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (checked) Color(0xFF4CAF50)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(visible = checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Принято",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Горизонтальная линейка дней с поддержкой скролла и свайпов. Центрируется вокруг выбранной даты.
 */
@Composable
fun SwipeableDaySelectorRow(
    selectedDate: String,
    todayClickTrigger: Int,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sdfDb = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val sdfDay = remember { SimpleDateFormat("EE", Locale("ru")) }
    val sdfNum = remember { SimpleDateFormat("d", Locale.US) }

    // Контролируем centerDate для скролла-стабилизации.
    var centerDate by remember { mutableStateOf(selectedDate) }

    // Генерируем 30 дней до и 30 дней после centerDate (всего 61 день).
    val daysList = remember(centerDate) {
        val list = mutableListOf<Triple<String, String, String>>() // dateStr, dayName, dayNum
        val cal = Calendar.getInstance()
        try {
            cal.time = sdfDb.parse(centerDate) ?: Date()
        } catch (_: Exception) {}

        cal.add(Calendar.DATE, -30)
        for (i in 0..60) {
            val dateStr = sdfDb.format(cal.time)
            val dayName = sdfDay.format(cal.time).uppercase()
            val dayNum = sdfNum.format(cal.time)
            list.add(Triple(dateStr, dayName, dayNum))
            cal.add(Calendar.DATE, 1)
        }
        list
    }

    val listState = rememberLazyListState()

    // Если дата выбрана за пределами текущего стабильного окна, смещаем центр
    LaunchedEffect(selectedDate) {
        val isInRange = daysList.any { it.first == selectedDate }
        if (!isInRange) {
            centerDate = selectedDate
        }
    }

    // Скроллим на выбранную дату (или принудительно центрируем при нажатии кнопки "Сегодня")
    LaunchedEffect(selectedDate, todayClickTrigger, daysList) {
        val targetIndex = daysList.indexOfFirst { it.first == selectedDate }
        if (targetIndex != -1) {
            listState.animateScrollToItem(maxOf(0, targetIndex - 2))
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(daysList) { (dateStr, dayName, dayNum) ->
            val isSelected = dateStr == selectedDate

            Column(
                modifier = Modifier
                    .width(55.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .clickable { onDateSelected(dateStr) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dayNum,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Диалог создания нового курса. Использует пикеры для выбора дат и классический ввод (TextField) для частоты.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, dosage: String, startDate: String, endDate: String, timesPerDay: Int, iconName: String, iconColor: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val sysToday = Calendar.getInstance()
    val todayString = sdf.format(sysToday.time)

    sysToday.add(Calendar.DATE, 7)
    val plusWeekString = sdf.format(sysToday.time)

    var startDate by remember { mutableStateOf(todayString) }
    var endDate by remember { mutableStateOf(plusWeekString) }

    var timesPerDayInput by remember { mutableStateOf("2") }
    var selectedIconName by remember { mutableStateOf("Medication") }
    var selectedColorName by remember { mutableStateOf("Blue") }

    var nameError by remember { mutableStateOf(false) }
    var dosageError by remember { mutableStateOf(false) }
    var frequencyError by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                text = "Добавить лекарство",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Название лекарства
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("Название (например: Ибупрофен)") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_name")
                )

                // Дозировка
                OutlinedTextField(
                    value = dosage,
                    onValueChange = {
                        dosage = it
                        if (it.isNotBlank()) dosageError = false
                    },
                    label = { Text("Дозировка (например: 1 таб, 5 мг)") },
                    isError = dosageError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_dosage")
                )

                // Дата начала (Клик открывает системный пикер)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { },
                        label = { Text("Дата начала курса") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Выбрать дату начала"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_input_start_date"),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Дата окончания (Клик открывает системный пикер)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEndDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { },
                        label = { Text("Дата окончания курса") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Выбрать дату окончания"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_input_end_date"),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Выбор иконки
                Text(
                    text = "Выберите иконку:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconsList = listOf(
                        "Medication" to Icons.Default.Medication,
                        "Favorite" to Icons.Default.Favorite,
                        "Star" to Icons.Default.Star,
                        "Info" to Icons.Default.Info,
                        "Warning" to Icons.Default.Warning
                    )
                    iconsList.forEach { (name, icon) ->
                        val isIconSelected = selectedIconName == name
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isIconSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { selectedIconName = name },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = name,
                                tint = if (isIconSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Выбор цвета
                Text(
                    text = "Выберите цвет:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colorsList = listOf(
                        "Blue" to Color(0xFF2196F3),
                        "Red" to Color(0xFFE53935),
                        "Green" to Color(0xFF4CAF50),
                        "Orange" to Color(0xFFFF9800),
                        "Purple" to Color(0xFF9C27B0),
                        "Pink" to Color(0xFFE91E63)
                    )
                    colorsList.forEach { (colorName, colorValue) ->
                        val isColorSelected = selectedColorName == colorName
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colorValue)
                                .clickable { selectedColorName = colorName },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isColorSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Выбран",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Частота приема: классическое поле ввода (требование пользователя)
                OutlinedTextField(
                    value = timesPerDayInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            timesPerDayInput = newValue
                            frequencyError = false
                        }
                    },
                    label = { Text("Раз(а) в день (приемы)") },
                    isError = frequencyError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_times_per_day")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    if (dosage.isBlank()) {
                        dosageError = true
                        return@Button
                    }
                    val freq = timesPerDayInput.toIntOrNull()
                    if (freq == null || freq <= 0 || freq > 12) {
                        frequencyError = true
                        return@Button
                    }
                    onConfirm(name, dosage, startDate, endDate, freq, selectedIconName, selectedColorName)
                },
                modifier = Modifier.testTag("dialog_button_confirm")
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() },
                modifier = Modifier.testTag("dialog_button_dismiss")
            ) {
                Text("Отмена")
            }
        }
    )

    if (showStartDatePicker) {
        val initialMillis = remember(startDate) {
            try {
                sdf.parse(startDate)?.time
            } catch (e: Exception) {
                null
            }
        }
        M3DatePickerDialog(
            initialDateMillis = initialMillis,
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { timeMillis ->
                if (timeMillis != null) {
                    val pickedSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    pickedSdf.timeZone = TimeZone.getTimeZone("UTC")
                    startDate = pickedSdf.format(Date(timeMillis))
                }
                showStartDatePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        val initialMillis = remember(endDate) {
            try {
                sdf.parse(endDate)?.time
            } catch (e: Exception) {
                null
            }
        }
        M3DatePickerDialog(
            initialDateMillis = initialMillis,
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { timeMillis ->
                if (timeMillis != null) {
                    val pickedSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    pickedSdf.timeZone = TimeZone.getTimeZone("UTC")
                    endDate = pickedSdf.format(Date(timeMillis))
                }
                showEndDatePicker = false
            }
        )
    }
}
