package mn.num.taskscheduler

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CreateProjectFragment : Fragment(R.layout.fragment_create_project) {

    // Edit mode эсэхийг шалгах хувьсагч
    private var isEditMode = false

    // Засварлах project-ийн id-г хадгална
    private var oldProjectId = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // UI элементүүдийг layout-аас авч байна
        val btnBackProject = view.findViewById<ImageButton>(R.id.btnBackProject)
        val etProjectName = view.findViewById<EditText>(R.id.etProjectName)
        val etProjectDescription = view.findViewById<EditText>(R.id.etProjectDescription)
        val btnSaveProject = view.findViewById<Button>(R.id.btnSaveProject)

        // Энэ fragment нь edit горимоор нээгдсэн эсэхийг arguments-аас уншина
        isEditMode = arguments?.getBoolean("is_edit_project_mode", false) ?: false

        // Хуучин project-ийн id-г arguments-аас авна
        oldProjectId = arguments?.getString("old_project_id").orEmpty()

        // Хэрвээ edit mode бол өмнөх утгуудыг form дээр харуулна
        if (isEditMode) {
            etProjectName.setText(arguments?.getString("old_project_title").orEmpty())
            etProjectDescription.setText(arguments?.getString("old_project_description").orEmpty())

            // Товчны текстийг update болгож өөрчилнө
            btnSaveProject.text = "UPDATE PROJECT"
        }

        // Back товч дарахад өмнөх fragment руу буцна
        btnBackProject.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Save / Update товч дарахад ажиллах логик
        btnSaveProject.setOnClickListener {

            // Хэрэглэгчийн оруулсан нэр, тайлбарыг авна
            val title = etProjectName.text.toString().trim()
            val description = etProjectDescription.text.toString().trim()

            // Project нэр хоосон бол анхааруулга өгнө
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Project name оруулна уу", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Одоогийн огноог project үүсгэсэн өдөр болгон хадгална
            val createdDate = SimpleDateFormat("MMM d yyyy", Locale.getDefault()).format(Date())

            // Хэрвээ edit mode бол хуучин project-ийг шинэчилнэ
            if (isEditMode) {

                // Бүх project-уудыг storage-оос уншиж байна
                val projects = ProjectStorage.getProjects(requireContext())

                // Засварлагдаж байгаа project-ийг id-аар нь олж авч байна
                val oldProject = projects.find { it.id == oldProjectId }

                // Шинэчлэгдсэн Project объект үүсгэнэ
                val updatedProject = Project(
                    id = oldProjectId,
                    title = title,
                    description = description,
                    createdDate = oldProject?.createdDate ?: createdDate,
                    totalTasks = oldProject?.totalTasks ?: 0,
                    completedTasks = oldProject?.completedTasks ?: 0
                )

                // Storage дээр project-ийг update хийнэ
                ProjectStorage.updateProject(requireContext(), oldProjectId, updatedProject)

                // Амжилттай шинэчлэгдсэн тухай мэдэгдэнэ
                Toast.makeText(requireContext(), "Project updated", Toast.LENGTH_SHORT).show()

            } else {
                // Шинэ project үүсгэх логик

                // Давтагдашгүй id үүсгэнэ
                val newProject = Project(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    description = description,
                    createdDate = createdDate,
                    totalTasks = 0,
                    completedTasks = 0
                )

                // Шинэ project-ийг storage-д хадгална
                ProjectStorage.addProject(requireContext(), newProject)

                // Амжилттай хадгалагдсан тухай мэдэгдэнэ
                Toast.makeText(requireContext(), "Project хадгалагдлаа", Toast.LENGTH_SHORT).show()
            }

            // Хадгалсны дараа HomeFragment руу буцаж очно
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }
    }
}