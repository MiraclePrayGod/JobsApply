package com.example.getjob.data.api

import com.example.getjob.data.models.requests.CreateSubscriptionRequest
import com.example.getjob.data.models.responses.SubscriptionResponse
import com.example.getjob.data.models.responses.SubscriptionStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SubscriptionApi {
    @POST("api/subscriptions/subscribe")
    suspend fun subscribe(@Body request: CreateSubscriptionRequest): Response<SubscriptionResponse>

    @GET("api/subscriptions/me/status")
    suspend fun getStatus(): Response<SubscriptionStatusResponse>

    @GET("api/subscriptions/me/history")
    suspend fun getHistory(): Response<List<SubscriptionResponse>>

    @POST("api/subscriptions/cancel")
    suspend fun cancel(): Response<Map<String, String>>
}

