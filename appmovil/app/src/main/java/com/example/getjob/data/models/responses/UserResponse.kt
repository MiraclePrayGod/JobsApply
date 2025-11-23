package com.example.getjob.data.models.responses

data class UserResponse(
    val id: Int,
    val email: String,
    val role: String,
    val full_name: String? = null,
    val phone: String? = null,
    val profile_image_url: String? = null,  // URL de foto de perfil
    val created_at: String,
    val updated_at: String
)

