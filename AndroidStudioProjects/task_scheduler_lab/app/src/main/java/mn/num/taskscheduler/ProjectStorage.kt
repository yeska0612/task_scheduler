package mn.num.taskscheduler

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Project мэдээллийг утсан дээр хадгалах storage class
// SharedPreferences + Gson ашиглан project list-г JSON хэлбэрээр хадгалдаг
object ProjectStorage {

    // SharedPreferences-ийн нэр
    private const val PREF_NAME = "project_prefs"

    // Project list хадгалах key
    private const val KEY_PROJECTS = "projects"

    // Project-уудын жагсаалтыг SharedPreferences-д хадгална
    fun saveProjects(context: Context, projects: List<Project>) {

        // SharedPreferences-г авч байна
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // Project list-г JSON string болгон хөрвүүлж байна
        val json = Gson().toJson(projects)

        // JSON-г SharedPreferences-д хадгалж байна
        prefs.edit().putString(KEY_PROJECTS, json).apply()
    }

    // SharedPreferences-оос project list-г уншина
    fun getProjects(context: Context): MutableList<Project> {

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // Хадгалсан JSON-г авч байна
        val json = prefs.getString(KEY_PROJECTS, null) ?: return mutableListOf()

        // JSON-г Project list болгон хөрвүүлж байна
        val type = object : TypeToken<MutableList<Project>>() {}.type

        return Gson().fromJson(json, type) ?: mutableListOf()
    }

    // Шинэ project нэмэх функц
    fun addProject(context: Context, project: Project) {

        // Одоогийн project list-г авч байна
        val projects = getProjects(context)

        // Шинэ project-г жагсаалтанд нэмнэ
        projects.add(project)

        // Шинэ жагсаалтыг дахин хадгална
        saveProjects(context, projects)
    }

    // Project мэдээллийг шинэчлэх функц
    fun updateProject(context: Context, oldProjectId: String, updatedProject: Project) {

        val projects = getProjects(context)

        // Засах project-ийн index-г олж байна
        val index = projects.indexOfFirst { it.id == oldProjectId }

        if (index != -1) {

            // Хуучин project-г шинэ project-оор солино
            projects[index] = updatedProject

            // Шинэ жагсаалтыг хадгална
            saveProjects(context, projects)
        }
    }

    // Project устгах функц
    fun deleteProject(context: Context, projectId: String) {

        val projects = getProjects(context)

        // Сонгосон project-г жагсаалтаас устгана
        projects.removeAll { it.id == projectId }

        // Шинэ жагсаалтыг хадгална
        saveProjects(context, projects)

        // Тухайн project-д хамаарах task-уудыг бас устгана
        val tasks = TaskStorage.getTasks(context)

        // ProjectId таарахгүй task-уудыг үлдээнэ
        val updatedTasks = tasks.filter { it.projectId != projectId }

        // Task list-г шинэчлэн хадгална
        TaskStorage.replaceAllTasks(context, updatedTasks)
    }
}