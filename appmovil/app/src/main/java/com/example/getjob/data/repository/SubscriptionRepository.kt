package com.example.getjob.data.repository

import com.example.getjob.data.api.ApiClient
import com.example.getjob.data.models.requests.CreateSubscriptionRequest
import com.example.getjob.data.models.responses.SubscriptionResponse
import com.example.getjob.data.models.responses.SubscriptionStatusResponse

class SubscriptionRepository {
    private val subscriptionApi = ApiClient.subscriptionApi
    
    suspend fun subscribe(plan: String, paymentCode: String): Result<SubscriptionResponse> {
        return try {
            val response = subscriptionApi.subscribe(CreateSubscriptionRequest(plan, paymentCode))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                val errorMessage = try {
                    val errorBody = response.errorBody()?.use { it.string() }
                    com.example.getjob.utils.ErrorParser.parseFastApiError(errorBody) 
                        ?: "Error ${response.code()}: ${response.message()}"
                } catch (e: Exception) {
                    "Error ${response.code()}: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = com.example.getjob.utils.ErrorParser.parseError(e)
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun getStatus(): Result<SubscriptionStatusResponse> {
        return try {
            val response = subscriptionApi.getStatus()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                val errorMessage = try {
                    val errorBody = response.errorBody()?.use { it.string() }
                    com.example.getjob.utils.ErrorParser.parseFastApiError(errorBody) 
                        ?: "Error ${response.code()}: ${response.message()}"
                } catch (e: Exception) {
                    "Error ${response.code()}: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = com.example.getjob.utils.ErrorParser.parseError(e)
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun getHistory(): Result<List<SubscriptionResponse>> {
        return try {
            val response = subscriptionApi.getHistory()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                val errorMessage = try {
                    val errorBody = response.errorBody()?.use { it.string() }
                    com.example.getjob.utils.ErrorParser.parseFastApiError(errorBody) 
                        ?: "Error ${response.code()}: ${response.message()}"
                } catch (e: Exception) {
                    "Error ${response.code()}: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = com.example.getjob.utils.ErrorParser.parseError(e)
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun cancel(): Result<Unit> {
        return try {
            val response = subscriptionApi.cancel()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = try {
                    val errorBody = response.errorBody()?.use { it.string() }
                    com.example.getjob.utils.ErrorParser.parseFastApiError(errorBody) 
                        ?: "Error ${response.code()}: ${response.message()}"
                } catch (e: Exception) {
                    "Error ${response.code()}: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = com.example.getjob.utils.ErrorParser.parseError(e)
            Result.failure(Exception(errorMessage))
        }
    }
}
