package com.example.getjob.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface LocationApi {
    /**
     * Actualiza la ubicación del trabajador en tiempo real
     */
    @POST("api/location/update")
    suspend fun updateLocation(@Body request: LocationUpdateRequest): Response<LocationUpdateResponse>
    
    /**
     * Actualiza la ubicación del trabajador para un trabajo específico
     */
    @POST("api/jobs/{jobId}/location")
    suspend fun updateJobLocation(
        @Path("jobId") jobId: Int,
        @Body request: LocationUpdateRequest
    ): Response<LocationUpdateResponse>
}

data class LocationUpdateRequest(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val speed: Float? = null
)

data class LocationUpdateResponse(
    val success: Boolean,
    val message: String? = null
)

