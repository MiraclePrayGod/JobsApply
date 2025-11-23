package com.example.getjob.data.models.responses

data class SenderInfo(
    val id: Int,
    val full_name: String? = null,
    val email: String
)

data class MessageResponse(
    val id: Int,
    val job_id: Int,
    val sender_id: Int,
    val content: String,
    val has_image: Boolean,
    val image_url: String? = null,
    val sender: SenderInfo? = null,
    val created_at: String
)

