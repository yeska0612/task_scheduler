package mn.num.taskscheduler

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateTaskFragment : Fragment(R.layout.fragment_create_task) {

    private var selectedCategory = "Life"
    private var selectedPriority = "Medium"
    private var selectedProjectId = ""

    private val selectedDateCalendar: Calendar = Calendar.getInstance()

    // Task-ийн start time
    private var startHour = 9
    private var startMinute = 0

    private var isEditMode = false
    private var oldTask: Task? = null

    // Reminder state
    private var reminderType = "NONE" // NONE / EXACT / AFTER_CREATE
    private var reminderLabel = "No reminder"
    private var reminderTriggerAtMillis = 0L

    // Exact reminder-д зориулсан calendar
    private val exactReminderCalendar: Calendar = Calendar.getInstance()

    // After create preset-үүд
    private val afterCreateOptions = listOf(
        "5 minutes",
        "15 minutes",
        "30 minutes",
        "1 hour",
        "1 day"
    )

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    requireContext(),
                    "Notification permission зөвшөөрөөгүй байна",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        val etTaskName = view.findViewById<EditText>(R.id.etTaskName)
        val etTaskDescription = view.findViewById<EditText>(R.id.etTaskDescription)

        val spProject = view.findViewById<Spinner>(R.id.spProject)

        val layoutDate = view.findViewById<LinearLayout>(R.id.layoutDate)
        val layoutTime = view.findViewById<LinearLayout>(R.id.layoutTime)

        val tvSelectedDate = view.findViewById<TextView>(R.id.tvSelectedDate)
        val tvSelectedTime = view.findViewById<TextView>(R.id.tvSelectedTime)

        val switchReminder = view.findViewById<Switch>(R.id.switchReminder)
        val layoutReminderOptions = view.findViewById<LinearLayout>(R.id.layoutReminderOptions)
        val spReminderType = view.findViewById<Spinner>(R.id.spReminderType)
        val layoutExactReminder = view.findViewById<LinearLayout>(R.id.layoutExactReminder)
        val tvExactReminderValue = view.findViewById<TextView>(R.id.tvExactReminderValue)
        val layoutAfterCreateReminder =
            view.findViewById<LinearLayout>(R.id.layoutAfterCreateReminder)
        val spAfterCreateDelay = view.findViewById<Spinner>(R.id.spAfterCreateDelay)
        val tvReminderPreview = view.findViewById<TextView>(R.id.tvReminderPreview)

        val btnCategoryLife = view.findViewById<MaterialButton>(R.id.btnCategoryLife)
        val btnCategoryWork = view.findViewById<MaterialButton>(R.id.btnCategoryWork)
        val btnCategoryStudy = view.findViewById<MaterialButton>(R.id.btnCategoryStudy)

        val btnPriorityLow = view.findViewById<MaterialButton>(R.id.btnPriorityLow)
        val btnPriorityMedium = view.findViewById<MaterialButton>(R.id.btnPriorityMedium)
        val btnPriorityHigh = view.findViewById<MaterialButton>(R.id.btnPriorityHigh)

        val btnCreateTask = view.findViewById<Button>(R.id.btnCreateTask)

        askNotificationPermissionIfNeeded()

        val projects = ProjectStorage.getProjects(requireContext())

        // Project spinner
        val projectOptions = mutableListOf("No Project")
        projectOptions.addAll(projects.map { it.title })

        val projectAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            projectOptions
        )
        projectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spProject.adapter = projectAdapter

        // Reminder type spinner
        val reminderTypeOptions = listOf("Exact", "After create")
        val reminderTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            reminderTypeOptions
        )
        reminderTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spReminderType.adapter = reminderTypeAdapter

        // After create spinner
        val afterCreateAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            afterCreateOptions
        )
        afterCreateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spAfterCreateDelay.adapter = afterCreateAdapter

        isEditMode = arguments?.getBoolean("is_edit_mode", false) ?: false
        val preselectedDateKey = arguments?.getString("preselected_date_key").orEmpty()

        if (isEditMode) {
            oldTask = Task(
                title = arguments?.getString("old_title").orEmpty(),
                description = arguments?.getString("old_description").orEmpty(),
                date = arguments?.getString("old_date").orEmpty(),
                time = arguments?.getString("old_time").orEmpty(),
                category = arguments?.getString("old_category") ?: "Life",
                priority = arguments?.getString("old_priority") ?: "Medium",
                remindMe = arguments?.getBoolean("old_remind_me", false) ?: false,
                reminderType = arguments?.getString("old_reminder_type") ?: "NONE",
                reminderLabel = arguments?.getString("old_reminder_label") ?: "No reminder",
                reminderTriggerAtMillis = arguments?.getLong("old_reminder_trigger_at", 0L) ?: 0L,
                isDone = arguments?.getBoolean("old_is_done", false) ?: false,
                projectId = arguments?.getString("old_project_id") ?: "",
                dateKey = arguments?.getString("old_date_key") ?: ""
            )

            etTaskName.setText(oldTask?.title)
            etTaskDescription.setText(oldTask?.description)
            tvSelectedDate.text = oldTask?.date

            selectedCategory = oldTask?.category ?: "Life"
            selectedPriority = oldTask?.priority ?: "Medium"
            selectedProjectId = oldTask?.projectId ?: ""

            val selectedIndex = projects.indexOfFirst { it.id == selectedProjectId }
            if (selectedIndex != -1) {
                spProject.setSelection(selectedIndex + 1) // +1 because No Project is first
            } else {
                spProject.setSelection(0)
            }

            syncDateFromTask(oldTask?.dateKey.orEmpty(), oldTask?.date.orEmpty())
            syncTimeFromText(oldTask?.time.orEmpty())
            updateDisplayedTime(tvSelectedTime)

            // Reminder сэргээх
            switchReminder.isChecked = oldTask?.remindMe ?: false
            reminderType = oldTask?.reminderType ?: "NONE"
            reminderLabel = oldTask?.reminderLabel ?: "No reminder"
            reminderTriggerAtMillis = oldTask?.reminderTriggerAtMillis ?: 0L

            restoreReminderUi(
                reminderType = reminderType,
                reminderLabel = reminderLabel,
                reminderTriggerAtMillis = reminderTriggerAtMillis,
                layoutReminderOptions = layoutReminderOptions,
                spReminderType = spReminderType,
                layoutExactReminder = layoutExactReminder,
                tvExactReminderValue = tvExactReminderValue,
                layoutAfterCreateReminder = layoutAfterCreateReminder,
                spAfterCreateDelay = spAfterCreateDelay,
                tvReminderPreview = tvReminderPreview
            )

            btnCreateTask.text = "UPDATE TASK"
        } else {
            if (preselectedDateKey.isNotEmpty()) {
                syncDateFromDateKey(preselectedDateKey)
            }

            updateDisplayedDate(tvSelectedDate)
            updateDisplayedTime(tvSelectedTime)
            selectedProjectId = ""
            spProject.setSelection(0)

            switchReminder.isChecked = false
            layoutReminderOptions.visibility = View.GONE
            updateReminderPreview(tvReminderPreview)
        }

        updateCategorySelection(btnCategoryLife, btnCategoryWork, btnCategoryStudy)
        updatePrioritySelection(btnPriorityLow, btnPriorityMedium, btnPriorityHigh)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        layoutDate.setOnClickListener {
            val year = selectedDateCalendar.get(Calendar.YEAR)
            val month = selectedDateCalendar.get(Calendar.MONTH)
            val day = selectedDateCalendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, y, m, d ->
                selectedDateCalendar.set(Calendar.YEAR, y)
                selectedDateCalendar.set(Calendar.MONTH, m)
                selectedDateCalendar.set(Calendar.DAY_OF_MONTH, d)
                updateDisplayedDate(tvSelectedDate)
            }, year, month, day).show()
        }

        layoutTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, min ->
                startHour = h
                startMinute = min
                updateDisplayedTime(tvSelectedTime)
            }, startHour, startMinute, true).show()
        }

        btnCategoryLife.setOnClickListener {
            selectedCategory = "Life"
            updateCategorySelection(btnCategoryLife, btnCategoryWork, btnCategoryStudy)
        }

        btnCategoryWork.setOnClickListener {
            selectedCategory = "Work"
            updateCategorySelection(btnCategoryLife, btnCategoryWork, btnCategoryStudy)
        }

        btnCategoryStudy.setOnClickListener {
            selectedCategory = "Study"
            updateCategorySelection(btnCategoryLife, btnCategoryWork, btnCategoryStudy)
        }

        btnPriorityLow.setOnClickListener {
            selectedPriority = "Low"
            updatePrioritySelection(btnPriorityLow, btnPriorityMedium, btnPriorityHigh)
        }

        btnPriorityMedium.setOnClickListener {
            selectedPriority = "Medium"
            updatePrioritySelection(btnPriorityLow, btnPriorityMedium, btnPriorityHigh)
        }

        btnPriorityHigh.setOnClickListener {
            selectedPriority = "High"
            updatePrioritySelection(btnPriorityLow, btnPriorityMedium, btnPriorityHigh)
        }

        // Reminder switch
        switchReminder.setOnCheckedChangeListener { _, isChecked ->
            layoutReminderOptions.visibility = if (isChecked) View.VISIBLE else View.GONE

            if (!isChecked) {
                reminderType = "NONE"
                reminderLabel = "No reminder"
                reminderTriggerAtMillis = 0L
                updateReminderPreview(tvReminderPreview)
            } else {
                // Default: Exact
                reminderType = "EXACT"
                spReminderType.setSelection(0)
                layoutExactReminder.visibility = View.VISIBLE
                layoutAfterCreateReminder.visibility = View.GONE
                if (reminderTriggerAtMillis == 0L || oldTask?.reminderType != "EXACT") {
                    reminderLabel = "Select reminder date & time"
                }
                tvExactReminderValue.text =
                    if (reminderType == "EXACT" && reminderLabel.startsWith("Exact:")) {
                        reminderLabel.removePrefix("Exact: ").trim()
                    } else {
                        "Select reminder date & time"
                    }
                updateReminderPreview(tvReminderPreview)
            }
        }

        // Reminder type spinner
        spReminderType.onItemSelectedListener = SimpleItemSelectedListener { position ->
            if (!switchReminder.isChecked) return@SimpleItemSelectedListener

            if (position == 0) {
                reminderType = "EXACT"
                layoutExactReminder.visibility = View.VISIBLE
                layoutAfterCreateReminder.visibility = View.GONE

                if (!reminderLabel.startsWith("Exact:")) {
                    reminderLabel = "Select reminder date & time"
                    reminderTriggerAtMillis = 0L
                    tvExactReminderValue.text = "Select reminder date & time"
                }
            } else {
                reminderType = "AFTER_CREATE"
                layoutExactReminder.visibility = View.GONE
                layoutAfterCreateReminder.visibility = View.VISIBLE

                if (!reminderLabel.contains("after create", ignoreCase = true)) {
                    val defaultDelay = afterCreateOptions[0]
                    reminderLabel = "$defaultDelay after create"
                    reminderTriggerAtMillis = 0L
                    spAfterCreateDelay.setSelection(0)
                }
            }

            updateReminderPreview(tvReminderPreview)
        }

        // Exact reminder click
        layoutExactReminder.setOnClickListener {
            if (!switchReminder.isChecked) return@setOnClickListener

            val year = exactReminderCalendar.get(Calendar.YEAR)
            val month = exactReminderCalendar.get(Calendar.MONTH)
            val day = exactReminderCalendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, y, m, d ->
                exactReminderCalendar.set(Calendar.YEAR, y)
                exactReminderCalendar.set(Calendar.MONTH, m)
                exactReminderCalendar.set(Calendar.DAY_OF_MONTH, d)

                TimePickerDialog(requireContext(), { _, h, min ->
                    exactReminderCalendar.set(Calendar.HOUR_OF_DAY, h)
                    exactReminderCalendar.set(Calendar.MINUTE, min)
                    exactReminderCalendar.set(Calendar.SECOND, 0)
                    exactReminderCalendar.set(Calendar.MILLISECOND, 0)

                    reminderType = "EXACT"
                    reminderTriggerAtMillis = exactReminderCalendar.timeInMillis
                    val formatted = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.getDefault()
                    ).format(exactReminderCalendar.time)

                    reminderLabel = "Exact: $formatted"
                    tvExactReminderValue.text = formatted
                    updateReminderPreview(tvReminderPreview)

                }, exactReminderCalendar.get(Calendar.HOUR_OF_DAY),
                    exactReminderCalendar.get(Calendar.MINUTE), true).show()

            }, year, month, day).show()
        }

        // After create delay spinner
        spAfterCreateDelay.onItemSelectedListener = SimpleItemSelectedListener { position ->
            if (!switchReminder.isChecked) return@SimpleItemSelectedListener
            if (reminderType != "AFTER_CREATE") return@SimpleItemSelectedListener

            val selectedDelay = afterCreateOptions[position]
            reminderLabel = "$selectedDelay after create"
            reminderTriggerAtMillis = 0L
            updateReminderPreview(tvReminderPreview)
        }

        btnCreateTask.setOnClickListener {
            val title = etTaskName.text.toString().trim()
            val description = etTaskDescription.text.toString().trim()
            val visibleDate = tvSelectedDate.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.task_name_required),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val selectedPosition = spProject.selectedItemPosition

            selectedProjectId = if (selectedPosition <= 0) {
                ""
            } else {
                projects[selectedPosition - 1].id
            }

            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(selectedDateCalendar.time)

            val startTime = formatTime(startHour, startMinute)

            val remindMe = switchReminder.isChecked

            // Final reminder info
            val finalReminderType: String
            val finalReminderLabel: String
            val finalReminderTriggerAtMillis: Long

            if (!remindMe) {
                finalReminderType = "NONE"
                finalReminderLabel = "No reminder"
                finalReminderTriggerAtMillis = 0L
            } else {
                if (reminderType == "EXACT") {
                    if (reminderTriggerAtMillis <= 0L) {
                        Toast.makeText(
                            requireContext(),
                            "Reminder date & time сонгоно уу",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    finalReminderType = "EXACT"
                    finalReminderLabel = reminderLabel
                    finalReminderTriggerAtMillis = reminderTriggerAtMillis
                } else {
                    val delayText = spAfterCreateDelay.selectedItem?.toString() ?: afterCreateOptions[0]
                    val offsetMillis = getDelayMillis(delayText)
                    finalReminderType = "AFTER_CREATE"
                    finalReminderLabel = "$delayText after create"
                    finalReminderTriggerAtMillis = System.currentTimeMillis() + offsetMillis
                }
            }

            val taskToSave = Task(
                title = title,
                description = description,
                date = visibleDate,
                time = startTime,
                category = selectedCategory,
                priority = selectedPriority,
                remindMe = remindMe,
                reminderType = finalReminderType,
                reminderLabel = finalReminderLabel,
                reminderTriggerAtMillis = finalReminderTriggerAtMillis,
                isDone = oldTask?.isDone ?: false,
                projectId = selectedProjectId,
                dateKey = dateKey
            )

            if (isEditMode && oldTask != null) {
                TaskStorage.updateTask(requireContext(), oldTask!!, taskToSave)

                if (remindMe) {
                    scheduleReminder(
                        taskTitle = title,
                        taskMessage = getString(R.string.task_reminder_message, title),
                        triggerAtMillis = finalReminderTriggerAtMillis
                    )
                }

                Toast.makeText(requireContext(), "Task updated", Toast.LENGTH_SHORT).show()
            } else {
                TaskStorage.addTask(requireContext(), taskToSave)

                if (remindMe) {
                    scheduleReminder(
                        taskTitle = title,
                        taskMessage = getString(R.string.task_reminder_message, title),
                        triggerAtMillis = finalReminderTriggerAtMillis
                    )
                }

                Toast.makeText(
                    requireContext(),
                    getString(R.string.task_saved),
                    Toast.LENGTH_SHORT
                ).show()
            }

            parentFragmentManager.popBackStack()
        }
    }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateDisplayedDate(tv: TextView) {
        val formatter = SimpleDateFormat("EEEE d, MMMM", Locale.getDefault())
        tv.text = formatter.format(selectedDateCalendar.time)
    }

    private fun updateDisplayedTime(tv: TextView) {
        tv.text = formatTime(startHour, startMinute)
    }

    private fun formatTime(hour: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    private fun syncDateFromTask(dateKey: String, visibleDateText: String) {
        if (dateKey.isNotEmpty()) {
            syncDateFromDateKey(dateKey)
        } else {
            syncDateFromVisibleText(visibleDateText)
        }
    }

    private fun syncDateFromDateKey(dateKey: String) {
        try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsedDate = formatter.parse(dateKey)
            if (parsedDate != null) {
                selectedDateCalendar.time = parsedDate
            }
        } catch (_: Exception) {
        }
    }

    private fun syncDateFromVisibleText(dateText: String) {
        try {
            val dateFormat = SimpleDateFormat("EEEE d, MMMM", Locale.getDefault())
            val parsedDate = dateFormat.parse(dateText)

            if (parsedDate != null) {
                val tempCalendar = Calendar.getInstance()
                tempCalendar.time = parsedDate

                selectedDateCalendar.set(Calendar.YEAR, tempCalendar.get(Calendar.YEAR))
                selectedDateCalendar.set(Calendar.MONTH, tempCalendar.get(Calendar.MONTH))
                selectedDateCalendar.set(Calendar.DAY_OF_MONTH, tempCalendar.get(Calendar.DAY_OF_MONTH))
            }
        } catch (_: Exception) {
        }
    }

    private fun syncTimeFromText(timeText: String) {
        try {
            val parts = timeText.split(":")
            if (parts.size == 2) {
                startHour = parts[0].trim().toInt()
                startMinute = parts[1].trim().toInt()
            }
        } catch (_: Exception) {
        }
    }

    private fun updateCategorySelection(
        life: MaterialButton,
        work: MaterialButton,
        study: MaterialButton
    ) {
        styleChip(life, selectedCategory == "Life", "#F2C9D7", "#E7AFC5")
        styleChip(work, selectedCategory == "Work", "#E9B8D0", "#D98BB2")
        styleChip(study, selectedCategory == "Study", "#EAB774", "#DA9B4C")
    }

    private fun updatePrioritySelection(
        low: MaterialButton,
        medium: MaterialButton,
        high: MaterialButton
    ) {
        styleChip(low, selectedPriority == "Low", "#C6E9C6", "#91D391")
        styleChip(medium, selectedPriority == "Medium", "#F0E098", "#E5D063")
        styleChip(high, selectedPriority == "High", "#F1C3C3", "#E69B9B")
    }

    private fun styleChip(
        button: MaterialButton,
        selected: Boolean,
        normalColor: String,
        selectedColor: String
    ) {
        val bgColor = if (selected) selectedColor else normalColor
        button.setBackgroundColor(android.graphics.Color.parseColor(bgColor))
        button.strokeWidth = if (selected) 3 else 1
        button.setTextColor(android.graphics.Color.parseColor("#333333"))
    }

    private fun updateReminderPreview(tvReminderPreview: TextView) {
        tvReminderPreview.text = when (reminderType) {
            "EXACT" -> reminderLabel
            "AFTER_CREATE" -> reminderLabel
            else -> "No reminder selected"
        }
    }

    private fun restoreReminderUi(
        reminderType: String,
        reminderLabel: String,
        reminderTriggerAtMillis: Long,
        layoutReminderOptions: LinearLayout,
        spReminderType: Spinner,
        layoutExactReminder: LinearLayout,
        tvExactReminderValue: TextView,
        layoutAfterCreateReminder: LinearLayout,
        spAfterCreateDelay: Spinner,
        tvReminderPreview: TextView
    ) {
        if (oldTask?.remindMe != true) {
            layoutReminderOptions.visibility = View.GONE
            this.reminderType = "NONE"
            this.reminderLabel = "No reminder"
            this.reminderTriggerAtMillis = 0L
            updateReminderPreview(tvReminderPreview)
            return
        }

        layoutReminderOptions.visibility = View.VISIBLE

        when (reminderType) {
            "EXACT" -> {
                spReminderType.setSelection(0)
                layoutExactReminder.visibility = View.VISIBLE
                layoutAfterCreateReminder.visibility = View.GONE

                if (reminderTriggerAtMillis > 0L) {
                    exactReminderCalendar.timeInMillis = reminderTriggerAtMillis
                }

                tvExactReminderValue.text =
                    if (reminderLabel.startsWith("Exact:")) {
                        reminderLabel.removePrefix("Exact: ").trim()
                    } else {
                        "Select reminder date & time"
                    }
            }

            "AFTER_CREATE" -> {
                spReminderType.setSelection(1)
                layoutExactReminder.visibility = View.GONE
                layoutAfterCreateReminder.visibility = View.VISIBLE

                val index = afterCreateOptions.indexOfFirst {
                    reminderLabel.contains(it, ignoreCase = true)
                }
                spAfterCreateDelay.setSelection(if (index != -1) index else 0)
            }

            else -> {
                layoutReminderOptions.visibility = View.GONE
            }
        }

        updateReminderPreview(tvReminderPreview)
    }

    private fun getDelayMillis(delayText: String): Long {
        return when (delayText) {
            "5 minutes" -> 5 * 60 * 1000L
            "15 minutes" -> 15 * 60 * 1000L
            "30 minutes" -> 30 * 60 * 1000L
            "1 hour" -> 60 * 60 * 1000L
            "1 day" -> 24 * 60 * 60 * 1000L
            else -> 5 * 60 * 1000L
        }
    }

    private fun scheduleReminder(taskTitle: String, taskMessage: String, triggerAtMillis: Long) {
        try {
            if (triggerAtMillis <= System.currentTimeMillis()) {
                Toast.makeText(
                    requireContext(),
                    "Өнгөрсөн хугацаанд reminder тохируулах боломжгүй",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(
                        requireContext(),
                        "Notification permission зөвшөөрнө үү",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
            }

            val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
                putExtra("task_title", taskTitle)
                putExtra("task_message", taskMessage)
            }

            val requestCode = System.currentTimeMillis().toInt()

            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager =
                requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )

            Toast.makeText(
                requireContext(),
                "Reminder амжилттай тохируулагдлаа",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Reminder тохируулах үед алдаа гарлаа: ${e.javaClass.simpleName}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}