package com.example.getjob.data.models.responses

data class WorkerResponse(
    val id: Int,
    val user_id: Int,
    val full_name: String,
    val phone: String?,
    val services: List<String>?,
    val description: String?,
    val district: String?,
    val is_available: Boolean,
    val yape_number: String?,
    val profile_image_url: String?,
    val is_verified: Boolean = false,
    val verification_photo_url: String? = null,
    val is_plus_active: Boolean = false, // Estado de Modo Plus
    val plus_expires_at: String? = null, // Fecha de expiración de Modo Plus (ISO format)
    val created_at: String,
    val updated_at: String
)

