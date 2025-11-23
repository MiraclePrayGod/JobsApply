package com.example.getjob.data.models.responses

data class ClientResponse(
    val id: Int,
    val user_id: Int,
    val full_name: String,
    val phone: String?,
    val district: String?
)

