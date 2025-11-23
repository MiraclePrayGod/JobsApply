package com.example.getjob.data.models.requests

import java.math.BigDecimal

data class AddExtraRequest(
    val extra_amount: BigDecimal,
    val description: String? = null
)

