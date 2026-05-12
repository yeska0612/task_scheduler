package mn.num.taskscheduler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// RecyclerView дээр project-уудыг харуулах adapter class
class ProjectAdapter(

    // RecyclerView дээр харагдах бүх project-ийн жагсаалт
    private val projects: List<Project>,

    // Project дээр нэг дарахад ажиллах callback
    private val onProjectClick: ((Project) -> Unit)? = null,

    // Project дээр удаан дарахад ажиллах callback
    private val onProjectLongClick: ((Project) -> Unit)? = null

) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    // ViewHolder нь нэг project item-ийн UI элементүүдийг хадгална
    class ProjectViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        // Project-ийн нэрийг харуулах TextView
        val title: TextView = view.findViewById(R.id.tvProjectTitle)

        // Project progress (жишээ нь 1/3 completed) харуулах TextView
        val progress: TextView = view.findViewById(R.id.tvProjectProgress)

        // Project үүсгэсэн огноог харуулах TextView
        val date: TextView = view.findViewById(R.id.tvProjectDate)
    }

    // RecyclerView шинэ item үүсгэх үед ажиллана
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {

        // item_project.xml layout-г inflate хийж project item үүсгэж байна
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project, parent, false)

        return ProjectViewHolder(view)
    }

    // RecyclerView item бүр дээр өгөгдлийг байрлуулна
    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {

        // Тухайн position дээрх project-ийг авч байна
        val project = projects[position]

        // Project-ийн нэрийг харуулна
        holder.title.text = project.title

        // Project-ийн progress-ийг харуулна
        holder.progress.text = "✓ ${project.completedTasks}/${project.totalTasks} completed"

        // Project үүсгэсэн огноог харуулна
        holder.date.text = project.createdDate

        // Project дээр нэг дарахад edit эсвэл detail callback ажиллана
        holder.itemView.setOnClickListener {
            onProjectClick?.invoke(project)
        }

        // Project дээр удаан дарахад delete callback ажиллана
        holder.itemView.setOnLongClickListener {
            onProjectLongClick?.invoke(project)
            true
        }
    }

    // RecyclerView дээр нийт хэдэн project item байгааг буцаана
    override fun getItemCount(): Int = projects.size
}