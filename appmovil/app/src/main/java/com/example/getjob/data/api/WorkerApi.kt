package com.example.getjob.data.api

import com.example.getjob.data.models.requests.WorkerRegisterRequest
import com.example.getjob.data.models.responses.WorkerResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST

interface WorkerApi {
    @POST("api/workers/register")
    suspend fun register(@Body request: WorkerRegisterRequest): Response<WorkerResponse>
    
    @GET("api/workers/me")
    suspend fun getMyProfile(): Response<WorkerResponse>
    
    @PUT("api/workers/me")
    suspend fun updateProfile(@Body request: WorkerUpdateRequest): Response<WorkerResponse>
    
    @POST("api/workers/me/verify")
    suspend fun submitVerification(@Body request: VerificationRequest): Response<WorkerResponse>
}

data class VerificationRequest(
    val verification_photo_url: String
)

data class WorkerUpdateRequest(
    val full_name: String? = null,
    val phone: String? = null,
    val services: List<String>? = null,
    val description: String? = null,
    val district: String? = null,
    val is_available: Boolean? = null,
    val yape_number: String? = null
)

