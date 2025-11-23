package com.example.getjob.data.models.responses

data class ClientInfo(
    val id: Int,
    val full_name: String? = null,
    val phone: String? = null,
    val email: String
)

