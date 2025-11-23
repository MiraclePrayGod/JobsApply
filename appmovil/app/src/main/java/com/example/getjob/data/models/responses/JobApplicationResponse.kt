package com.example.getjob.data.models.responses

data class JobApplicationResponse(
    val id: Int,
    val job_id: Int,
    val worker_id: Int,
    val is_accepted: Boolean,
    val created_at: String,
    val updated_at: String,
    val worker: WorkerInfo? = null  // Información del trabajador
)

