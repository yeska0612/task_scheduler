package mn.num.taskscheduler

// Нэг task-ийн мэдээллийг хадгалах model class
data class Task(
    val title: String,
    val description: String,
    val date: String,                  // Хэрэглэгчид харагдах огноо
    val time: String,                  // Одоо зөвхөн start time хадгална
    val category: String,
    val priority: String,

    // Reminder тохиргоо
    val remindMe: Boolean,
    val reminderType: String,          // NONE / EXACT / AFTER_CREATE
    val reminderLabel: String,         // UI дээр харагдах текст
    val reminderTriggerAtMillis: Long, // Notification дуугарах яг хугацаа

    val isDone: Boolean,
    val projectId: String,
    val dateKey: String                // Calendar filter хийх стандарт огноо (yyyy-MM-dd)
)