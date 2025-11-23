package com.example.getjob.data.models.responses

data class AuthResponse(
    val access_token: String,
    val token_type: String,
    val user: UserInfo
)

data class UserInfo(
    val id: Int,
    val email: String,
    val role: String
)

