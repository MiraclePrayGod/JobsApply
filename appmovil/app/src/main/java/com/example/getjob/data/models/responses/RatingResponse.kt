package com.example.getjob.data.models.responses

data class RatingResponse(
    val id: Int,
    val job_id: Int,
    val worker_rating: Int? = null,
    val worker_comment: String? = null,
    val client_rating: Int? = null,
    val client_comment: String? = null,
    val created_at: String
)

