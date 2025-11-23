package com.example.getjob.data.models.requests

data class CreateSubscriptionRequest(
    val plan: String,        // "daily" o "weekly"
    val payment_code: String // código simulado de Yape
)

