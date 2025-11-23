package com.example.getjob.data.api

import com.example.getjob.data.models.responses.CommissionResponse
import retrofit2.Response
import retrofit2.http.*
import retrofit2.http.POST
import retrofit2.http.Path

interface CommissionApi {
    @GET("api/commissions/my-commissions")
    suspend fun getMyCommissions(): Response<List<CommissionResponse>>
    
    @GET("api/commissions/pending")
    suspend fun getPendingCommissions(): Response<List<CommissionResponse>>
    
    @POST("api/commissions/{commissionId}/submit-payment")
    suspend fun submitPayment(
        @Path("commissionId") commissionId: Int,
        @Body request: com.example.getjob.data.models.requests.CommissionSubmitPaymentRequest
    ): Response<CommissionResponse>
    
    @GET("api/commissions/history")
    suspend fun getCommissionHistory(): Response<List<CommissionResponse>>
}

