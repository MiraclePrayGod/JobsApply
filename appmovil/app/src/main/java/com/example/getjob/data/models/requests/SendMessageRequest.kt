package com.example.getjob.data.models.requests

data class SendMessageRequest(
    val job_id: Int,
    val content: String,
    val has_image: Boolean = false,
    val image_url: String? = null,
    val application_id: Int? = null
)

