package com.example.getjob.data.models.requests

data class CommissionSubmitPaymentRequest(
    val payment_code: String,
    val payment_proof_url: String? = null
)

