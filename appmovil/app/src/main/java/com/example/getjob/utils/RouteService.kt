package com.example.getjob.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Servicio para calcular rutas y ETA usando OSRM (Open Source Routing Machine)
 * Similar a Google Maps - calcula ruta real y tiempo estimado
 */
class RouteService {
    // OSRM es un servicio público gratuito para calcular rutas
    private val osrmBaseUrl = "https://router.project-osrm.org"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    /**
     * Datos de una ruta calculada
     */
    data class RouteResult(
        val distance: Float, // en metros
        val duration: Int, // en segundos
        val polyline: List<Pair<Double, Double>> // puntos de la ruta
    )
    
    /**
     * Calcula la ruta entre dos puntos y retorna distancia, tiempo y puntos de la ruta
     * Similar a Google Maps Directions API
     */
    suspend fun calculateRoute(
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double
    ): RouteResult? = withContext(Dispatchers.IO) {
        try {
            // Formato OSRM: /route/v1/driving/{lon1},{lat1};{lon2},{lat2}
            val url = "$osrmBaseUrl/route/v1/driving/$originLon,$originLat;$destLon,$destLat?overview=full&geometries=geojson"
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val json = JSONObject(responseBody)
                    
                    if (json.getString("code") == "Ok" && json.has("routes")) {
                        val routes = json.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val route = routes.getJSONObject(0)
                            val distance = route.getDouble("distance").toFloat() // metros
                            val duration = route.getInt("duration") // segundos
                            
                            // Extraer geometría (puntos de la ruta)
                            val geometry = route.getJSONObject("geometry")
                            val coordinates = geometry.getJSONArray("coordinates")
                            val polyline = mutableListOf<Pair<Double, Double>>()
                            
                            for (i in 0 until coordinates.length()) {
                                val coord = coordinates.getJSONArray(i)
                                val lon = coord.getDouble(0)
                                val lat = coord.getDouble(1)
                                polyline.add(Pair(lat, lon))
                            }
                            
                            return@withContext RouteResult(distance, duration, polyline)
                        }
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Formatea el tiempo estimado en formato legible (ej: "15 min", "1h 30 min")
     */
    fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes}min"
            minutes > 0 -> "${minutes} min"
            else -> "< 1 min"
        }
    }
    
    /**
     * Formatea la distancia en formato legible
     */
    fun formatDistance(meters: Float): String {
        return when {
            meters < 1000 -> "${meters.toInt()} m"
            else -> String.format("%.2f km", meters / 1000)
        }
    }
}

