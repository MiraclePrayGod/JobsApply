package com.example.getjob.data.repository

import com.example.getjob.data.api.ApiClient
import com.example.getjob.data.models.requests.WorkerRegisterRequest
import com.example.getjob.data.models.responses.WorkerResponse
import com.example.getjob.utils.ErrorParser
import retrofit2.Response

class WorkerRepository {
    private val workerApi = ApiClient.workerApi
    
    suspend fun registerWorkerProfile(request: WorkerRegisterRequest): Result<WorkerResponse> {
        return try {
            val response = workerApi.register(request)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                val errorMessage = try {
                    val errorBody = response.errorBody()?.use { it.string() }
                    ErrorParser.parseFastApiError(errorBody) ?: "Error ${response.code()}: ${response.message()}"
                } catch (e: Exception) {
                    "Error ${response.code()}: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = ErrorParser.parseError(e)
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun getMyProfile(): Result<WorkerResponse> {
        return try {
            val response = workerApi.getMyProfile()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                val errorMessage = try {
                    val errorBody = response.errorBody()?.use { it.string() }
                    ErrorParser.parseFastApiError(errorBody) ?: "Error ${response.code()}: ${response.message()}"
                } catch (e: Exception) {
                    "Error ${response.code()}: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = ErrorParser.parseError(e)
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun submitVerification(verificationPhotoUrl: String): Result<WorkerResponse> {
        return try {
            val response = workerApi.submitVerification(
                com.example.getjob.data.api.VerificationRequest(verificationPhotoUrl)
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                val errorMessage = try {
                    val errorBody = response.errorBody()?.use { it.string() }
                    ErrorParser.parseFastApiError(errorBody) ?: "Error ${response.code()}: ${response.message()}"
                } catch (e: Exception) {
                    "Error ${response.code()}: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = ErrorParser.parseError(e)
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun updateProfile(
        fullName: String? = null,
        phone: String? = null,
        services: List<String>? = null,
        description: String? = null,
        district: String? = null,
        isAvailable: Boolean? = null,
        yapeNumber: String? = null,
        profileImageUrl: String? = null
    ): Result<WorkerResponse> {
        return try {
            val request = com.example.getjob.data.api.WorkerUpdateRequest(
                full_name = fullName,
                phone = phone,
                services = services,
                description = description,
                district = district,
                is_available = isAvailable,
                yape_number = yapeNumber
            )
            val response = workerApi.updateProfile(request)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                val errorMessage = try {
                    val errorBody = response.errorBody()?.use { it.string() }
                    ErrorParser.parseFastApiError(errorBody) ?: "Error ${response.code()}: ${response.message()}"
                } catch (e: Exception) {
                    "Error ${response.code()}: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = ErrorParser.parseError(e)
            Result.failure(Exception(errorMessage))
        }
    }
}

