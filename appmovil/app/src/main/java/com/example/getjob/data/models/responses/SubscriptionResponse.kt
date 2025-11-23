package com.example.getjob.data.models.responses

data class SubscriptionResponse(
    val id: Int,
    val plan: String,
    val days: Int,
    val amount: String,
    val status: String,
    val valid_from: String,
    val valid_until: String,
    val created_at: String
)

data class SubscriptionStatusResponse(
    val is_plus_active: Boolean,
    val plus_expires_at: String?,
    val current_plan: SubscriptionResponse?
)

