package com.example.getjob.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Servicio para convertir direcciones en coordenadas (Geocodificación)
 * Usa Nominatim de OpenStreetMap (gratuito, sin API key)
 */
class GeocodingService {
    private val nominatimBaseUrl = "https://nominatim.openstreetmap.org"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    /**
     * Convierte una dirección en coordenadas (latitud, longitud)
     * @param address Dirección completa (ej: "Juliaca/Mercado Barbara, Perú")
     * @return Pair<latitud, longitud> o null si no se encuentra
     */
    suspend fun geocodeAddress(address: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            if (address.isBlank()) {
                android.util.Log.w("GeocodingService", "Dirección vacía")
                return@withContext null
            }
            
            // URL encode la dirección
            val encodedAddress = java.net.URLEncoder.encode(address, "UTF-8")
            val url = "$nominatimBaseUrl/search?q=$encodedAddress&format=json&limit=1"
            
            android.util.Log.d("GeocodingService", "Geocodificando: $address")
            android.util.Log.d("GeocodingService", "URL: $url")
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "ServiFast-Android-App/1.0") // Requerido por Nominatim
                .addHeader("Accept-Language", "es") // Preferir español
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            android.util.Log.d("GeocodingService", "Respuesta código: ${response.code}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                android.util.Log.d("GeocodingService", "Respuesta body: $responseBody")
                
                if (responseBody != null && responseBody.isNotBlank()) {
                    val jsonArray = JSONArray(responseBody)
                    if (jsonArray.length() > 0) {
                        val firstResult = jsonArray.getJSONObject(0)
                        val lat = firstResult.getDouble("lat")
                        val lon = firstResult.getDouble("lon")
                        android.util.Log.d("GeocodingService", "Coordenadas obtenidas: lat=$lat, lon=$lon")
                        return@withContext Pair(lat, lon)
                    } else {
                        android.util.Log.w("GeocodingService", "No se encontraron resultados")
                    }
                } else {
                    android.util.Log.w("GeocodingService", "Respuesta vacía")
                }
            } else {
                android.util.Log.e("GeocodingService", "Error en respuesta: ${response.code} - ${response.message}")
            }
            
            null
        } catch (e: Exception) {
            android.util.Log.e("GeocodingService", "Error en geocodificación: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Obtiene coordenadas desde la ubicación actual del dispositivo
     * (Usa LocationService para obtener GPS)
     */
    suspend fun getCurrentLocationCoordinates(
        locationService: LocationService
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            val location = locationService.getCurrentLocation()
            if (location != null) {
                return@withContext Pair(location.latitude, location.longitude)
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Convierte coordenadas en una dirección (Reverse Geocoding)
     * @param latitude Latitud
     * @param longitude Longitud
     * @return Dirección formateada o null si no se encuentra
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        try {
            val url = "$nominatimBaseUrl/reverse?lat=$latitude&lon=$longitude&format=json&addressdetails=1&accept-language=es"
            
            android.util.Log.d("GeocodingService", "Reverse geocodificando: lat=$latitude, lon=$longitude")
            android.util.Log.d("GeocodingService", "URL: $url")
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "ServiFast-Android-App/1.0")
                .addHeader("Accept-Language", "es")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            android.util.Log.d("GeocodingService", "Reverse geocodificación código: ${response.code}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                android.util.Log.d("GeocodingService", "Reverse geocodificación body: $responseBody")
                
                if (responseBody != null && responseBody.isNotBlank()) {
                    val json = JSONObject(responseBody)
                    if (json.has("address")) {
                        val address = json.getJSONObject("address")
                        
                        // Construir dirección desde los componentes
                        val displayName = json.optString("display_name", "")
                        if (displayName.isNotBlank()) {
                            android.util.Log.d("GeocodingService", "Dirección obtenida: $displayName")
                            return@withContext displayName
                        }
                        
                        // Si no hay display_name, construir desde componentes
                        val parts = mutableListOf<String>()
                        address.optString("road", "")?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                        address.optString("house_number", "")?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                        address.optString("suburb", "")?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                        address.optString("city", "")?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                        address.optString("state", "")?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                        address.optString("country", "")?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                        
                        if (parts.isNotEmpty()) {
                            val formattedAddress = parts.joinToString(", ")
                            android.util.Log.d("GeocodingService", "Dirección construida: $formattedAddress")
                            return@withContext formattedAddress
                        }
                    }
                }
            } else {
                android.util.Log.e("GeocodingService", "Error en reverse geocodificación: ${response.code} - ${response.message}")
            }
            
            null
        } catch (e: Exception) {
            android.util.Log.e("GeocodingService", "Error en reverse geocodificación: ${e.message}", e)
            null
        }
    }
}

