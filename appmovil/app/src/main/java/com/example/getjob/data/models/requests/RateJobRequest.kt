package com.example.getjob.data.models.requests

data class RateJobRequest(
    val rating: Int, // 1-5
    val comment: String? = null
)

