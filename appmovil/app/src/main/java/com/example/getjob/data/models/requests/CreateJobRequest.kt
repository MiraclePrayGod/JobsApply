package com.example.getjob.data.models.requests

import java.math.BigDecimal

data class CreateJobRequest(
    val title: String,
    val description: String? = null,
    val service_type: String, // "Plomería", "Electricidad", etc.
    val payment_method: String, // "yape" o "cash"
    val base_fee: BigDecimal,
    val address: String,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val scheduled_at: String? = null // ISO 8601 format
)

