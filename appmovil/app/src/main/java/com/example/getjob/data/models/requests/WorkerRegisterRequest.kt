package com.example.getjob.data.models.requests

data class WorkerRegisterRequest(
    val user_id: Int,
    val full_name: String,
    val phone: String? = null,
    val services: List<String>? = null,
    val description: String? = null,
    val district: String? = null,
    val is_available: Boolean = false,
    val yape_number: String? = null,
    val profile_image_url: String? = null
)

