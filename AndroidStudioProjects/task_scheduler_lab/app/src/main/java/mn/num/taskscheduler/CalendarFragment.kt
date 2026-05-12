package mn.num.taskscheduler

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    private lateinit var rvCalendarTasks: RecyclerView
    private lateinit var tvSelectedDaySubtitle: TextView
    private lateinit var tvNoTasksForDay: TextView

    // Сонгосон өдрийн yyyy-MM-dd форматтай түлхүүр
    private var selectedDateKey: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBackCalendar = view.findViewById<ImageButton>(R.id.btnBackCalendar)
        val calendarView = view.findViewById<android.widget.CalendarView>(R.id.calendarView)
        val cardAddTaskForDay = view.findViewById<CardView>(R.id.cardAddTaskForDay)
        tvSelectedDaySubtitle = view.findViewById(R.id.tvSelectedDaySubtitle)
        tvNoTasksForDay = view.findViewById(R.id.tvNoTasksForDay)
        rvCalendarTasks = view.findViewById(R.id.rvCalendarTasks)

        rvCalendarTasks.layoutManager = LinearLayoutManager(requireContext())
        rvCalendarTasks.setHasFixedSize(false)

        // Эхлэх үед өнөөдрийн өдрийг сонгосон байдалтай болгоно
        val today = Date()
        selectedDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today)
        tvSelectedDaySubtitle.text =
            SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(today)

        loadTasksForSelectedDate()

        // Буцах товч
        btnBackCalendar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Calendar дээр өдөр сонгох
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            selectedDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(selectedCalendar.time)

            tvSelectedDaySubtitle.text =
                SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
                    .format(selectedCalendar.time)

            loadTasksForSelectedDate()
        }

        // Сонгосон өдөрт task нэмэх
        cardAddTaskForDay.setOnClickListener {
            val fragment = CreateTaskFragment().apply {
                arguments = Bundle().apply {
                    putString("preselected_date_key", selectedDateKey)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadTasksForSelectedDate() {
        val allTasks = TaskStorage.getTasks(requireContext())

        val filteredTasks = allTasks.filter { it.dateKey == selectedDateKey }
        val finalTaskList = filteredTasks.toMutableList()

        tvNoTasksForDay.visibility =
            if (finalTaskList.isEmpty()) View.VISIBLE else View.GONE

        lateinit var adapter: OngoingTaskAdapter

        adapter = OngoingTaskAdapter(
            tasks = finalTaskList,

            // Task дээр дарахад edit mode-оор орно
            onTaskClick = { task ->
                val fragment = CreateTaskFragment().apply {
                    arguments = Bundle().apply {
                        putBoolean("is_edit_mode", true)
                        putString("old_title", task.title)
                        putString("old_description", task.description)
                        putString("old_date", task.date)
                        putString("old_time", task.time)
                        putString("old_category", task.category)
                        putString("old_priority", task.priority)
                        putBoolean("old_remind_me", task.remindMe)
                        putBoolean("old_is_done", task.isDone)
                        putString("old_project_id", task.projectId)
                        putString("old_date_key", task.dateKey)
                        putString("old_reminder_type", task.reminderType)
                        putString("old_reminder_label", task.reminderLabel)
                        putLong("old_reminder_trigger_at", task.reminderTriggerAtMillis)
                    }
                }

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            },

            // Устгах
            onTaskLongClick = { task, position ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete task")
                    .setMessage("“${task.title}” task-ийг устгах уу?")
                    .setPositiveButton("Yes") { _, _ ->
                        finalTaskList.removeAt(position)

                        val allCurrentTasks = TaskStorage.getTasks(requireContext()).toMutableList()
                        val removeIndex = allCurrentTasks.indexOfFirst {
                            it.title == task.title &&
                                    it.description == task.description &&
                                    it.date == task.date &&
                                    it.time == task.time &&
                                    it.category == task.category &&
                                    it.priority == task.priority &&
                                    it.remindMe == task.remindMe &&
                                    it.isDone == task.isDone &&
                                    it.projectId == task.projectId &&
                                    it.dateKey == task.dateKey
                        }

                        if (removeIndex != -1) {
                            allCurrentTasks.removeAt(removeIndex)
                            TaskStorage.replaceAllTasks(requireContext(), allCurrentTasks)
                        }

                        Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
                        loadTasksForSelectedDate()
                    }
                    .setNegativeButton("No", null)
                    .show()
            },

            // Done / not done toggle
            onToggleDone = { task, _ ->
                val allCurrentTasks = TaskStorage.getTasks(requireContext()).toMutableList()
                val index = allCurrentTasks.indexOfFirst {
                    it.title == task.title &&
                            it.description == task.description &&
                            it.date == task.date &&
                            it.time == task.time &&
                            it.category == task.category &&
                            it.priority == task.priority &&
                            it.remindMe == task.remindMe &&
                            it.isDone == task.isDone &&
                            it.projectId == task.projectId &&
                            it.dateKey == task.dateKey
                }

                if (index != -1) {
                    val updatedTask = allCurrentTasks[index].copy(isDone = !allCurrentTasks[index].isDone)
                    allCurrentTasks[index] = updatedTask
                    TaskStorage.replaceAllTasks(requireContext(), allCurrentTasks)

                    Toast.makeText(
                        requireContext(),
                        if (updatedTask.isDone) "Task marked as done" else "Task marked as not done",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadTasksForSelectedDate()
                }
            }
        )

        rvCalendarTasks.adapter = adapter
    }
}