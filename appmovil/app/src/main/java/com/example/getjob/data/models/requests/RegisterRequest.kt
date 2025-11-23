package com.example.getjob.data.models.requests

data class RegisterRequest(
    val email: String,
    val password: String,
    val role: String,  // "client" o "worker"
    val full_name: String? = null,
    val phone: String? = null
)

