package com.example.getjob.data.api

import com.example.getjob.data.models.requests.AddExtraRequest
import com.example.getjob.data.models.requests.CreateJobRequest
import com.example.getjob.data.models.requests.RateJobRequest
import com.example.getjob.data.models.responses.JobResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface JobApi {
    @POST("api/jobs")
    suspend fun createJob(@Body request: CreateJobRequest): Response<JobResponse>
    
    @GET("api/jobs/available")
    suspend fun getAvailableJobs(
        @Query("service_type") serviceType: String? = null,
        @Query("search") search: String? = null
    ): Response<List<JobResponse>>
    
    @GET("api/jobs/my-jobs")
    suspend fun getMyJobs(): Response<List<JobResponse>>
    
    @GET("api/jobs/{jobId}")
    suspend fun getJobById(@Path("jobId") jobId: Int): Response<JobResponse>
    
    @POST("api/jobs/{jobId}/apply")
    suspend fun applyToJob(@Path("jobId") jobId: Int): Response<JobResponse>
    
    @POST("api/jobs/{jobId}/accept-worker/{applicationId}")
    suspend fun acceptWorker(@Path("jobId") jobId: Int, @Path("applicationId") applicationId: Int): Response<JobResponse>
    
    @GET("api/jobs/{jobId}/applications")
    suspend fun getJobApplications(@Path("jobId") jobId: Int): Response<List<com.example.getjob.data.models.responses.JobApplicationResponse>>
    
    @GET("api/jobs/my-applications")
    suspend fun getMyApplications(): Response<List<com.example.getjob.data.models.responses.JobApplicationResponse>>
    
    @POST("api/jobs/{jobId}/start-route")
    suspend fun startRoute(@Path("jobId") jobId: Int): Response<JobResponse>
    
    @POST("api/jobs/{jobId}/confirm-arrival")
    suspend fun confirmArrival(@Path("jobId") jobId: Int): Response<JobResponse>
    
    @POST("api/jobs/{jobId}/start-service")
    suspend fun startService(@Path("jobId") jobId: Int): Response<JobResponse>
    
    @POST("api/jobs/{jobId}/add-extra")
    suspend fun addExtra(@Path("jobId") jobId: Int, @Body request: AddExtraRequest): Response<JobResponse>
    
    @POST("api/jobs/{jobId}/complete")
    suspend fun completeJob(@Path("jobId") jobId: Int): Response<JobResponse>
    
    @POST("api/jobs/{jobId}/rate")
    suspend fun rateJob(@Path("jobId") jobId: Int, @Body request: RateJobRequest): Response<com.example.getjob.data.models.responses.RatingResponse>
    
    @POST("api/jobs/{jobId}/rate-worker")
    suspend fun rateWorker(@Path("jobId") jobId: Int, @Body request: RateJobRequest): Response<com.example.getjob.data.models.responses.RatingResponse>
    
    @GET("api/jobs/{jobId}/rating")
    suspend fun getJobRating(@Path("jobId") jobId: Int): Response<com.example.getjob.data.models.responses.RatingResponse>
    
    @POST("api/jobs/{jobId}/cancel")
    suspend fun cancelJob(@Path("jobId") jobId: Int): Response<JobResponse>
}

