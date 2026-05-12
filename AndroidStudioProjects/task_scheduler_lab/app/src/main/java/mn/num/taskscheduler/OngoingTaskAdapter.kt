package mn.num.taskscheduler

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

// RecyclerView дээр task-уудыг харуулах adapter
class OngoingTaskAdapter(
    private val tasks: MutableList<Task>,

    // Task дээр дарахад ажиллах callback
    private val onTaskClick: ((Task) -> Unit)? = null,

    // Task дээр удаан дарахад ажиллах callback
    private val onTaskLongClick: ((Task, Int) -> Unit)? = null,

    // Done / Not Done toggle хийх callback
    private val onToggleDone: ((Task, Int) -> Unit)? = null

) : RecyclerView.Adapter<OngoingTaskAdapter.TaskViewHolder>() {

    // ViewHolder нь RecyclerView-ийн нэг мөр (item)-ийн UI элементүүдийг хадгална
    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        // Task card
        val cardTask: CardView = view.findViewById(R.id.cardTask)

        // Task мэдээлэл
        val title: TextView = view.findViewById(R.id.tvTaskTitle)
        val description: TextView = view.findViewById(R.id.tvTaskDescription)
        val date: TextView = view.findViewById(R.id.tvTaskDate)
        val time: TextView = view.findViewById(R.id.tvTaskTime)
        val category: TextView = view.findViewById(R.id.tvTaskCategory)
        val priority: TextView = view.findViewById(R.id.tvTaskPriority)

        // Timeline indicator элементүүд
        val viewTopLine: View = view.findViewById(R.id.viewTopLine)
        val viewBottomLine: View = view.findViewById(R.id.viewBottomLine)

        // Done / Not Done circle indicator
        val viewTaskIndicator: View = view.findViewById(R.id.viewTaskIndicator)
    }

    // RecyclerView шинэ item үүсгэх үед ажиллана
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {

        // item_ongoing_task.xml layout-г inflate хийж байна
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ongoing_task, parent, false)

        return TaskViewHolder(view)
    }

    // RecyclerView item бүр дээр өгөгдөл байрлуулах
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {

        val task = tasks[position]

        // Task мэдээллийг UI дээр байрлуулж байна
        holder.title.text = task.title.ifEmpty { "No title" }
        holder.description.text = task.description.ifEmpty { "No description" }
        holder.date.text = task.date.ifEmpty { "-" }
        holder.time.text = task.time.ifEmpty { "-" }
        holder.category.text = task.category.ifEmpty { "Life" }
        holder.priority.text = task.priority.ifEmpty { "Medium" }

        // Timeline line visibility
        holder.viewTopLine.visibility =
            if (position == 0) View.INVISIBLE else View.VISIBLE

        holder.viewBottomLine.visibility =
            if (position == tasks.lastIndex) View.INVISIBLE else View.VISIBLE

        // Task дууссан эсэхээс хамаарч indicator өөрчилнө
        if (task.isDone) {

            // Done үед ногоон өнгөтэй болно
            holder.viewTaskIndicator.setBackgroundColor(Color.parseColor("#8BC34A"))

            // Text бага зэрэг бүдгэрнэ
            holder.title.alpha = 0.6f
            holder.description.alpha = 0.6f

        } else {

            // Not done үед default background
            holder.viewTaskIndicator.setBackgroundResource(R.drawable.bg_task_indicator)

            holder.title.alpha = 1f
            holder.description.alpha = 1f
        }

        // Priority-ийн өнгө
        when (task.priority.lowercase()) {
            "high" -> holder.priority.setTextColor(Color.parseColor("#D96C6C"))
            "medium" -> holder.priority.setTextColor(Color.parseColor("#B89000"))
            "low" -> holder.priority.setTextColor(Color.parseColor("#5A9C5A"))
            else -> holder.priority.setTextColor(Color.parseColor("#333333"))
        }

        // Category-ийн өнгө
        when (task.category.lowercase()) {
            "work" -> holder.category.setTextColor(Color.parseColor("#C06C93"))
            "study" -> holder.category.setTextColor(Color.parseColor("#C48A3A"))
            "life" -> holder.category.setTextColor(Color.parseColor("#333333"))
            else -> holder.category.setTextColor(Color.parseColor("#333333"))
        }

        // Task дээр дарахад edit screen рүү орох callback
        holder.cardTask.setOnClickListener {

            if (onTaskClick != null) {
                onTaskClick.invoke(task)
            } else {

                // Callback байхгүй үед түр Toast харуулна
                Toast.makeText(
                    holder.itemView.context,
                    "Task: ${task.title}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Task дээр удаан дарахад delete callback ажиллана
        holder.cardTask.setOnLongClickListener {

            if (onTaskLongClick != null) {
                onTaskLongClick.invoke(task, position)
            }

            true
        }

        // Done / Not Done circle дээр дарахад toggle callback ажиллана
        holder.viewTaskIndicator.setOnClickListener {

            if (onToggleDone != null) {
                onToggleDone.invoke(task, position)
            }
        }
    }

    // RecyclerView-д хэдэн item байгааг буцаана
    override fun getItemCount(): Int = tasks.size
}