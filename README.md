# SmartPills 💊

[![Android Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat-square&logo=android)](https://developer.android.com)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-1.9.0-7F52FF.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4.svg?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

**SmartPills** — современное, интуитивно понятное и функциональное мобильное приложение для Android, созданное для того, чтобы помочь вам контролировать приём лекарств, витаминов и биологически активных добавок (БАД). Приложение предоставляет гибкие возможности планирования курсов лечения и автоматически рассчитывает статистику их выполнения.

---

## 📸 Скриншоты интерфейса

*(Здесь вы можете разместить скриншоты вашего приложения)*

<div align="center">
  <table width="100%">
    <tr>
      <td width="50%" align="center">
        <b>Главный экран (Дашборд)</b><br/>
        <img src="assets/dashboard.png" width="300" alt="Главный экран SmartPills" />
      </td>
      <td width="50%" align="center">
        <b>Добавление лекарства</b><br/>
        <img src="assets/add_medication.png" width="300" alt="Добавление нового курса" />
      </td>
    </tr>
  </table>
</div>

---

## ✨ Ключевые возможности

- 📅 **Интерактивный календарь свайпов**: Удобная горизонтальная лента дней недели позволяет мгновенно переключаться между датами и просматривать назначения.
- 🔄 **Автоматическое планирование**: Вы указываете даты начала и окончания курса, а также количество приёмов в день — приложение само генерирует точное расписание.
- 🌅 **Группировка по фазам суток**: Для лучшей наглядности все запланированные приемы автоматически разбиваются на логические категории: *Утро*, *День*, *Вечер* и *Ночь*.
- 📈 **Контроль прогресса**:
  - Анимированный линейный индикатор выполнения для выбранного дня.
  - Реактивный подсчет общей статистики приверженности лечению за текущую неделю.
- 🔔 **Умные уведомления**:
  - Точные напоминания о приёме через системный `AlarmManager`.
  - Автоматическое восстановление будильников после перезагрузки телефона (`BootReceiver`).
  - При отметке таблетки как «Принято» предстоящий будильник для неё автоматически отменяется.

---

## 🛠 Технологический стек

Приложение разработано в соответствии с современными рекомендациями Android-разработки:

*   **Язык программирования**: [Kotlin](https://kotlinlang.org/)
*   **UI-фреймворк**: [Jetpack Compose](https://developer.android.com/jetpack/compose) с компонентами **Material 3**
*   **Архитектурный паттерн**: MVVM (Model-View-ViewModel) с чистым разделением слоев
*   **База данных**: [Room Database](https://developer.android.com/training/data-storage/room) (SQLite) с поддержкой связей «один ко многим»
*   **Асинхронность и реактивность**: Kotlin Coroutines & Flow (StateFlow, flatMapLatest)
*   **Уведомления**: AlarmManager, BroadcastReceiver, NotificationCompat

---

## 🚀 Как запустить проект локально

### Требования
*   [Android Studio](https://developer.android.com/studio) (рекомендуется версия Ladybug 2024.2.1 или новее)
*   Android SDK версии 26 (Android 8.0) и выше
*   Установленная JVM (Java Development Kit 17+)

### Инструкция по установке

1.  **Клонируйте репозиторий:**
    ```bash
    git clone https://github.com/Mandarln4ik/SmartPills.git
    cd SmartPills
    ```

2.  **Импортируйте проект:**
    Откройте Android Studio, выберите **Open** и выберите папку с данным проектом. Дождитесь окончания синхронизации Gradle.

3.  **Сборка и запуск:**
    Подключите физическое Android-устройство или запустите эмулятор, затем нажмите кнопку **Run (Shift + F10)** в Android Studio.
