package mn.num.taskscheduler

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// TaskStorage нь task мэдээллийг утсанд хадгалах storage class юм.
// SharedPreferences + Gson ашиглан task-уудыг JSON хэлбэрээр хадгална.
object TaskStorage {

    // SharedPreferences нэр
    private const val PREF_NAME = "task_scheduler_prefs"

    // Task list хадгалах key
    private const val KEY_TASKS = "tasks"

    // JSON-оос унших үед null утга гарч болзошгүй тул
    // түр зуур ашиглах class
    private data class StoredTask(
        val title: String? = null,
        val description: String? = null,
        val date: String? = null,
        val time: String? = null,
        val category: String? = null,
        val priority: String? = null,

        val remindMe: Boolean? = null,
        val reminderType: String? = null,
        val reminderLabel: String? = null,
        val reminderTriggerAtMillis: Long? = null,

        val isDone: Boolean? = null,
        val projectId: String? = null,
        val dateKey: String? = null
    )

    // Task list-ийг SharedPreferences-д хадгалах функц
    fun saveTasks(context: Context, tasks: List<Task>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // Task list → JSON болгоно
        val json = Gson().toJson(tasks)

        // JSON-г SharedPreferences-д хадгална
        prefs.edit().putString(KEY_TASKS, json).apply()
    }

    // SharedPreferences-ээс task list унших функц
    fun getTasks(context: Context): MutableList<Task> {
        return try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

            val json = prefs.getString(KEY_TASKS, null) ?: return mutableListOf()

            // JSON → StoredTask list болгоно
            val type = object : TypeToken<MutableList<StoredTask>>() {}.type

            val storedTasks: MutableList<StoredTask> =
                Gson().fromJson(json, type) ?: mutableListOf()

            // StoredTask → Task model болгоно
            storedTasks.map {
                Task(
                    title = it.title ?: "",
                    description = it.description ?: "",
                    date = it.date ?: "",
                    time = it.time ?: "",
                    category = it.category ?: "Life",
                    priority = it.priority ?: "Medium",

                    remindMe = it.remindMe ?: false,
                    reminderType = it.reminderType ?: "NONE",
                    reminderLabel = it.reminderLabel ?: "No reminder",
                    reminderTriggerAtMillis = it.reminderTriggerAtMillis ?: 0L,

                    isDone = it.isDone ?: false,
                    projectId = it.projectId ?: "",
                    dateKey = it.dateKey ?: ""
                )
            }.toMutableList()

        } catch (e: Exception) {
            // Алдаа гарвал хоосон list буцаана
            mutableListOf()
        }
    }

    // Шинэ task нэмэх функц
    fun addTask(context: Context, task: Task) {
        val tasks = getTasks(context)

        // Шинэ task list-ийн эхэнд нэмнэ
        tasks.add(0, task)

        saveTasks(context, tasks)
    }

    // Task update хийх функц
    fun updateTask(context: Context, oldTask: Task, updatedTask: Task) {
        val tasks = getTasks(context)

        // Хуучин task-г list дотроос хайна
        val index = tasks.indexOfFirst {
            it.title == oldTask.title &&
                    it.description == oldTask.description &&
                    it.date == oldTask.date &&
                    it.time == oldTask.time &&
                    it.category == oldTask.category &&
                    it.priority == oldTask.priority &&
                    it.remindMe == oldTask.remindMe &&
                    it.reminderType == oldTask.reminderType &&
                    it.reminderLabel == oldTask.reminderLabel &&
                    it.reminderTriggerAtMillis == oldTask.reminderTriggerAtMillis &&
                    it.isDone == oldTask.isDone &&
                    it.projectId == oldTask.projectId &&
                    it.dateKey == oldTask.dateKey
        }

        // Олдвол шинэ task-аар солино
        if (index != -1) {
            tasks[index] = updatedTask
            saveTasks(context, tasks)
        }
    }

    // Бүх task list-ийг шинэ list-ээр солих функц
    fun replaceAllTasks(context: Context, tasks: List<Task>) {
        saveTasks(context, tasks)
    }

    // Бүх task-г устгах функц
    fun clearTasks(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_TASKS).apply()
    }
}