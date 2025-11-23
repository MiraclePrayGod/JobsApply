package com.example.getjob.data.api

import com.example.getjob.data.models.requests.LoginRequest
import com.example.getjob.data.models.requests.RegisterRequest
import com.example.getjob.data.models.responses.AuthResponse
import com.example.getjob.data.models.responses.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserResponse>
    
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @GET("api/auth/me")
    suspend fun getCurrentUser(): Response<UserResponse>
    
    @PUT("api/auth/me")
    suspend fun updateProfile(@Body request: UserUpdateRequest): Response<UserResponse>
}

data class UserUpdateRequest(
    val email: String? = null,
    val password: String? = null,
    val full_name: String? = null,
    val phone: String? = null
)

