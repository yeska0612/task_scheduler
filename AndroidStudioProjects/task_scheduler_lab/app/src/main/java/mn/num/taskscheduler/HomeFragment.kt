package mn.num.taskscheduler

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Дээд хэсэгт бодит огноог харуулна
        val tvDate = view.findViewById<TextView>(R.id.tvDate)
        val currentDate = SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault()).format(Date())
        tvDate.text = currentDate

        // ---------------- PROJECTS ХЭСЭГ ----------------

        // Project-уудыг харуулах RecyclerView
        val rvProjects = view.findViewById<RecyclerView>(R.id.rvProjects)

        // Storage-оос хадгалсан project-уудыг уншиж авна
        val savedProjects = ProjectStorage.getProjects(requireContext())

        // Storage-оос хадгалсан task-уудыг уншиж авна
        val savedTasksForProjects = TaskStorage.getTasks(requireContext())

        // Project бүрийн progress-ийг task-уудаас автоматаар тооцоолно
        val projectList = savedProjects.map { project ->

            // Тухайн project-д хамаарах task-уудыг шүүж авч байна
            val projectTasks = savedTasksForProjects.filter { it.projectId == project.id }

            // Нийт task-ийн тоо
            val totalTasks = projectTasks.size

            // Дууссан task-ийн тоо
            val completedTasks = projectTasks.count { it.isDone }

            // Project объектын progress мэдээллийг шинэчилж буцаана
            project.copy(
                totalTasks = totalTasks,
                completedTasks = completedTasks
            )
        }

        // Project RecyclerView-ийн adapter
        val projectAdapter = ProjectAdapter(
            projects = projectList,

            // Project дээр нэг дарахад edit screen рүү орно
            onProjectClick = { project ->
                val fragment = CreateProjectFragment().apply {
                    arguments = Bundle().apply {
                        putBoolean("is_edit_project_mode", true)
                        putString("old_project_id", project.id)
                        putString("old_project_title", project.title)
                        putString("old_project_description", project.description)
                    }
                }

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            },

            // Project дээр удаан дарахад delete dialog гарна
            onProjectLongClick = { project ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete project")
                    .setMessage("“${project.title}” project-ийг устгах уу?")
                    .setPositiveButton("Yes") { _, _ ->

                        // Storage-оос project-ийг устгана
                        ProjectStorage.deleteProject(requireContext(), project.id)

                        Toast.makeText(requireContext(), "Project deleted", Toast.LENGTH_SHORT).show()

                        // HomeFragment-ийг дахин ачаалж шинэчлэгдсэн жагсаалтыг харуулна
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, HomeFragment())
                            .commit()
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        )

        // Project RecyclerView-ийн layout manager-ийг horizontal scroll-оор тохируулна
        rvProjects.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        // Adapter-ийг RecyclerView-д холбоно
        rvProjects.adapter = projectAdapter

        // ---------------- TASKS ХЭСЭГ ----------------

        // Task-уудыг харуулах RecyclerView
        val rvOngoingTasks = view.findViewById<RecyclerView>(R.id.rvOngoingTasks)

        // Storage-оос хадгалсан task-уудыг уншиж авна
        val savedTasks = TaskStorage.getTasks(requireContext())

        // Mutable list болгон хувиргаж байна
        val finalTaskList = savedTasks.toMutableList()

        // Adapter-ийг дараа нь callback дотор ашиглах тул lateinit болгож зарлав
        lateinit var ongoingTaskAdapter: OngoingTaskAdapter

        // Task RecyclerView-ийн adapter
        ongoingTaskAdapter = OngoingTaskAdapter(
            tasks = finalTaskList,

            // Task дээр нэг дарахад edit screen рүү орно
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

                        putString("old_reminder_type", task.reminderType)
                        putString("old_reminder_label", task.reminderLabel)
                        putLong("old_reminder_trigger_at", task.reminderTriggerAtMillis)

                        putBoolean("old_is_done", task.isDone)
                        putString("old_project_id", task.projectId)
                        putString("old_date_key", task.dateKey)
                    }
                }

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            },

            // Task дээр удаан дарахад delete dialog гарна
            onTaskLongClick = { task, position ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete task")
                    .setMessage("“${task.title}” task-ийг устгах уу?")
                    .setPositiveButton("Yes") { _, _ ->

                        // Жагсаалтаас task-ийг устгана
                        finalTaskList.removeAt(position)

                        // RecyclerView-г шинэчилнэ
                        ongoingTaskAdapter.notifyItemRemoved(position)
                        ongoingTaskAdapter.notifyItemRangeChanged(position, finalTaskList.size)

                        // Шинэ task list-ийг storage дээр хадгална
                        TaskStorage.replaceAllTasks(requireContext(), finalTaskList)

                        Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()

                        // HomeFragment-ийг дахин ачаалж шинэчилнэ
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, HomeFragment())
                            .commit()
                    }
                    .setNegativeButton("No", null)
                    .show()
            },

            // Done / not done circle дээр дарахад task төлөв солигдоно
            onToggleDone = { task, position ->

                // isDone утгыг эсрэг болгож шинэ task объект үүсгэнэ
                val updatedTask = task.copy(isDone = !task.isDone)

                // Жагсаалтын тухайн байршил дээр шинэ task-ийг солино
                finalTaskList[position] = updatedTask

                // RecyclerView-ийн тухайн мөрийг шинэчилнэ
                ongoingTaskAdapter.notifyItemChanged(position)

                // Шинэ жагсаалтыг storage-д хадгална
                TaskStorage.replaceAllTasks(requireContext(), finalTaskList)

                Toast.makeText(
                    requireContext(),
                    if (updatedTask.isDone) "Task marked as done" else "Task marked as not done",
                    Toast.LENGTH_SHORT
                ).show()

                // Project progress шинэчлэгдэхийн тулд HomeFragment-ийг дахин ачаална
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, HomeFragment())
                    .commit()
            }
        )

        // Task RecyclerView-ийн layout manager-ийг vertical байдлаар тохируулна
        rvOngoingTasks.layoutManager = LinearLayoutManager(requireContext())
        rvOngoingTasks.isNestedScrollingEnabled = false
        rvOngoingTasks.setHasFixedSize(false)

        // Adapter-ийг RecyclerView-д холбоно
        rvOngoingTasks.adapter = ongoingTaskAdapter

        // ---------------- CREATE PROJECT ----------------

        // Create Project card дээр дарахад шинэ project үүсгэх fragment нээгдэнэ
        val cardCreateProject = view.findViewById<CardView>(R.id.cardCreateProject)
        cardCreateProject.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CreateProjectFragment())
                .addToBackStack(null)
                .commit()
        }

        val cardOpenCalendar = view.findViewById<CardView>(R.id.cardOpenCalendar)

        // ---------------- CREATE TASK ----------------

        // Create Task card дээр дарахад шинэ task үүсгэх fragment нээгдэнэ
        val cardCreateTask = view.findViewById<CardView>(R.id.cardCreateTask)
        cardCreateTask.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CreateTaskFragment())
                .addToBackStack(null)
                .commit()
        }

        // ---------------- OPEN CALENDAR ----------------

        // Open Calendar card дээр дарахад CalendarFragment нээгдэнэ
        cardOpenCalendar.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CalendarFragment())
                .addToBackStack(null)
                .commit()
        }
        // ---------------- SEE MORE ----------------

        // See more текстүүдийг авч байна
        val tvSeeMoreProjects = view.findViewById<TextView>(R.id.tvSeeMoreProjects)
        val tvSeeMoreTasks = view.findViewById<TextView>(R.id.tvSeeMoreTasks)

        // Projects See more дээр дарахад түр toast харуулж байна
        tvSeeMoreProjects.setOnClickListener {
            Toast.makeText(requireContext(), "Projects бүгд харагдана", Toast.LENGTH_SHORT).show()
        }

        // Tasks See more дээр дарахад түр toast харуулж байна
        tvSeeMoreTasks.setOnClickListener {
            Toast.makeText(requireContext(), "Tasks бүгд харагдана", Toast.LENGTH_SHORT).show()
        }
    }
}