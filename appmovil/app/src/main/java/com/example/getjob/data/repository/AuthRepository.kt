package com.example.getjob.data.repository

import com.example.getjob.data.api.ApiClient
import com.example.getjob.data.models.requests.LoginRequest
import com.example.getjob.data.models.requests.RegisterRequest
import com.example.getjob.data.models.responses.AuthResponse
import com.example.getjob.data.models.responses.UserResponse
import com.example.getjob.utils.ErrorParser
import retrofit2.Response

class AuthRepository {
    private val authApi = ApiClient.authApi
    
    suspend fun register(
        email: String, 
        password: String, 
        role: String,
        fullName: String? = null,
        phone: String? = null
    ): Result<UserResponse> {
        return try {
            // Normalizar email (lowercase, trim) para alinearse con el backend
            val normalizedEmail = email.lowercase().trim()
            val response = authApi.register(RegisterRequest(normalizedEmail, password, role, fullName, phone))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                // Intentar leer el mensaje de error del body si existe
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
    
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            // Normalizar email (lowercase, trim) para alinearse con el backend
            val normalizedEmail = email.lowercase().trim()
            val response = authApi.login(LoginRequest(normalizedEmail, password))
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
    
    suspend fun getCurrentUser(): Result<UserResponse> {
        return try {
            val response = authApi.getCurrentUser()
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
        email: String? = null,
        password: String? = null,
        fullName: String? = null,
        phone: String? = null
    ): Result<UserResponse> {
        return try {
            // Normalizar email si se proporciona (lowercase, trim) para alinearse con el backend
            val normalizedEmail = email?.lowercase()?.trim()
            val request = com.example.getjob.data.api.UserUpdateRequest(
                email = normalizedEmail,
                password = password,
                full_name = fullName,
                phone = phone
            )
            val response = authApi.updateProfile(request)
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

