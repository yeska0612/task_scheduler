package mn.num.taskscheduler

// Project-ийн мэдээллийг хадгалах data class
// Энэ class нь нэг project-ийн бүх үндсэн мэдээллийг агуулна
data class Project(

    // Project-ийн давтагдашгүй ID
    // UUID ашиглан үүсгэдэг
    val id: String,

    // Project-ийн нэр
    val title: String,

    // Project-ийн тайлбар
    val description: String = "",

    // Project үүсгэсэн огноо
    val createdDate: String = "",

    // Тухайн project доторх нийт task-ийн тоо
    val totalTasks: Int = 0,

    // Дууссан task-ийн тоо
    val completedTasks: Int = 0
)