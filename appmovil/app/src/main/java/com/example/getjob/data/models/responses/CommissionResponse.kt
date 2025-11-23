package com.example.getjob.data.models.responses

data class JobInfo(
    val id: Int,
    val title: String,
    val payment_method: String
)

data class CommissionResponse(
    val id: Int,
    val job_id: Int,
    val worker_id: Int,
    val amount: Double,
    val payment_method: String, // "yape" or "cash" - del job
    val status: String, // "pending", "payment_submitted", "approved", "rejected"
    val payment_code: String?,
    val payment_proof_url: String?,
    val submitted_at: String?,
    val reviewed_at: String?,
    val reviewed_by: Int?,
    val notes: String?,
    val created_at: String,
    val updated_at: String,
    val job: JobInfo? = null // Información del trabajo
)

