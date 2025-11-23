package com.example.getjob.data.api

import com.example.getjob.data.models.requests.SendMessageRequest
import com.example.getjob.data.models.responses.MessageResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {
    @GET("api/chat/{jobId}/messages")
    suspend fun getMessages(
        @Path("jobId") jobId: Int,
        @Query("application_id") applicationId: Int? = null
    ): Response<List<MessageResponse>>
    
    @POST("api/chat/{jobId}/send")
    suspend fun sendMessage(
        @Path("jobId") jobId: Int,
        @Body request: SendMessageRequest
    ): Response<MessageResponse>
}

