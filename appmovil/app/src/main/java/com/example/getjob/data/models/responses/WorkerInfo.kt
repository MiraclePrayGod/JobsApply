package com.example.getjob.data.models.responses

data class WorkerInfo(
    val id: Int,
    val full_name: String? = null,
    val phone: String? = null,
    val profile_image_url: String? = null,
    val is_verified: Boolean = false
)

