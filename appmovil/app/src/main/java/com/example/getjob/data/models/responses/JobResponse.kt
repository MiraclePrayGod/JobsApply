package com.example.getjob.data.models.responses

import java.math.BigDecimal

data class JobResponse(
    val id: Int,
    val client_id: Int,
    val worker_id: Int? = null,
    val title: String,
    val description: String? = null,
    val service_type: String,
    val status: String,
    val payment_method: String,
    val base_fee: BigDecimal,
    val extras: BigDecimal,
    val total_amount: BigDecimal,
    val address: String,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val scheduled_at: String? = null,
    val started_at: String? = null,
    val completed_at: String? = null,
    val created_at: String,
    val updated_at: String,
    val client: ClientInfo? = null,  // Información del cliente
    val worker: WorkerInfo? = null  // Información del trabajador
)

