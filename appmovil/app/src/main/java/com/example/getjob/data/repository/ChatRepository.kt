package com.example.getjob.data.repository

import com.example.getjob.data.api.ApiClient
import com.example.getjob.data.models.requests.SendMessageRequest
import com.example.getjob.data.models.responses.MessageResponse

class ChatRepository {
    private val chatApi = ApiClient.chatApi
    
    suspend fun getMessages(jobId: Int, applicationId: Int? = null): Result<List<MessageResponse>> {
        return try {
            val response = chatApi.getMessages(jobId, applicationId)
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
    
    suspend fun sendMessage(jobId: Int, content: String, hasImage: Boolean = false, imageUrl: String? = null, applicationId: Int? = null): Result<MessageResponse> {
        return try {
            val request = SendMessageRequest(jobId, content, hasImage, imageUrl, applicationId)
            val response = chatApi.sendMessage(jobId, request)
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
}

