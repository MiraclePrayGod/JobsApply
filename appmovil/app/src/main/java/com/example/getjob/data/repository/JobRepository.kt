package com.example.getjob.data.repository

import com.example.getjob.data.api.ApiClient
import com.example.getjob.data.models.requests.AddExtraRequest
import com.example.getjob.data.models.requests.CreateJobRequest
import com.example.getjob.data.models.requests.RateJobRequest
import com.example.getjob.data.models.responses.JobResponse
import com.example.getjob.data.models.responses.RatingResponse
import com.example.getjob.data.models.responses.JobApplicationResponse

class JobRepository {
    private val jobApi = ApiClient.jobApi
    
    suspend fun createJob(request: CreateJobRequest): Result<JobResponse> {
        return try {
            val response = jobApi.createJob(request)
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
    
    suspend fun getAvailableJobs(serviceType: String? = null, searchQuery: String? = null): Result<List<JobResponse>> {
        return try {
            val response = jobApi.getAvailableJobs(serviceType, searchQuery)
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
    
    suspend fun getMyJobs(): Result<List<JobResponse>> {
        return try {
            val response = jobApi.getMyJobs()
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
    
    suspend fun getJobById(jobId: Int): Result<JobResponse> {
        return try {
            val response = jobApi.getJobById(jobId)
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
    
    suspend fun applyToJob(jobId: Int): Result<JobResponse> {
        return try {
            val response = jobApi.applyToJob(jobId)
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
    
    suspend fun acceptWorker(jobId: Int, applicationId: Int): Result<JobResponse> {
        android.util.Log.d("JobRepository", "📤 acceptWorker() - jobId: $jobId, applicationId: $applicationId")
        return try {
            android.util.Log.d("JobRepository", "🌐 Llamando API: POST /api/jobs/$jobId/accept-worker/$applicationId")
            val response = jobApi.acceptWorker(jobId, applicationId)
            android.util.Log.d("JobRepository", "📥 Respuesta recibida - Code: ${response.code()}, Success: ${response.isSuccessful}")
            
            val body = response.body()
            if (response.isSuccessful && body != null) {
                android.util.Log.d("JobRepository", "✅ Trabajador aceptado - Job Status: ${body.status}, Worker ID: ${body.worker_id}")
                Result.success(body)
            } else {
                val errorMessage = try {
                    val errorBody = response.errorBody()?.use { it.string() }
                    android.util.Log.e("JobRepository", "❌ Error en respuesta - Code: ${response.code()}, Body: $errorBody")
                    com.example.getjob.utils.ErrorParser.parseFastApiError(errorBody) 
                        ?: "Error ${response.code()}: ${response.message()}"
                } catch (e: Exception) {
                    android.util.Log.e("JobRepository", "❌ Error parseando errorBody: ${e.message}", e)
                    "Error ${response.code()}: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            android.util.Log.e("JobRepository", "❌ Excepción en acceptWorker: ${e.message}", e)
            val errorMessage = com.example.getjob.utils.ErrorParser.parseError(e)
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun getJobApplications(jobId: Int): Result<List<JobApplicationResponse>> {
        return try {
            val response = jobApi.getJobApplications(jobId)
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
    
    suspend fun getMyApplications(): Result<List<JobApplicationResponse>> {
        return try {
            val response = jobApi.getMyApplications()
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
    
    suspend fun startRoute(jobId: Int): Result<JobResponse> {
        return try {
            val response = jobApi.startRoute(jobId)
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
    
    suspend fun confirmArrival(jobId: Int): Result<JobResponse> {
        return try {
            val response = jobApi.confirmArrival(jobId)
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
    
    suspend fun startService(jobId: Int): Result<JobResponse> {
        return try {
            val response = jobApi.startService(jobId)
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
    
    suspend fun addExtra(jobId: Int, extraAmount: java.math.BigDecimal, description: String? = null): Result<JobResponse> {
        return try {
            val request = AddExtraRequest(extraAmount, description)
            val response = jobApi.addExtra(jobId, request)
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
    
    suspend fun cancelJob(jobId: Int): Result<JobResponse> {
        return try {
            val response = jobApi.cancelJob(jobId)
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
    
    suspend fun completeJob(jobId: Int): Result<JobResponse> {
        return try {
            val response = jobApi.completeJob(jobId)
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
    
    suspend fun rateJob(jobId: Int, rating: Int, comment: String? = null): Result<RatingResponse> {
        return try {
            val request = RateJobRequest(rating, comment)
            val response = jobApi.rateJob(jobId, request)
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
    
    suspend fun rateWorker(jobId: Int, rating: Int, comment: String? = null): Result<RatingResponse> {
        return try {
            val request = RateJobRequest(rating, comment)
            val response = jobApi.rateWorker(jobId, request)
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
    
    suspend fun getJobRating(jobId: Int): Result<RatingResponse?> {
        return try {
            val response = jobApi.getJobRating(jobId)
            if (response.isSuccessful) {
                // Si hay calificación, retornarla; si no (404), retornar null
                Result.success(response.body())
            } else {
                // Si es 404, no hay calificación (no es un error)
                if (response.code() == 404) {
                    Result.success(null)
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
            }
        } catch (e: Exception) {
            val errorMessage = com.example.getjob.utils.ErrorParser.parseError(e)
            Result.failure(Exception(errorMessage))
        }
    }
}

