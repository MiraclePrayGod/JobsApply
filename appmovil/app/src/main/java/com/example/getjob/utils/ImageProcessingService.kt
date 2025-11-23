package com.example.getjob.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Servicio para procesar imágenes: convertir Uri a Bitmap, comprimir, convertir a base64, etc.
 * Toda la lógica pesada de procesamiento de imágenes se hace aquí, fuera de la UI.
 */
class ImageProcessingService(private val context: Context) {
    
    /**
     * Convierte un Uri de imagen a Bitmap
     * @param uri Uri de la imagen
     * @return Bitmap o null si hay error
     */
    suspend fun uriToBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IOException) {
            android.util.Log.e("ImageProcessing", "Error al leer imagen desde Uri", e)
            null
        } catch (e: Exception) {
            android.util.Log.e("ImageProcessing", "Error inesperado al procesar imagen", e)
            null
        }
    }
    
    /**
     * Convierte un Bitmap a base64 string para envío al servidor
     * @param bitmap Bitmap a convertir
     * @param quality Calidad de compresión (0-100), default 85
     * @param format Formato de compresión (JPEG o PNG)
     * @return String base64 o null si hay error
     */
    suspend fun bitmapToBase64(
        bitmap: Bitmap,
        quality: Int = 85,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ): String? = withContext(Dispatchers.IO) {
        try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(format, quality, outputStream)
            val imageBytes = outputStream.toByteArray()
            Base64.encodeToString(imageBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            android.util.Log.e("ImageProcessing", "Error al convertir bitmap a base64", e)
            null
        }
    }
    
    /**
     * Convierte un Uri directamente a base64 string
     * Útil para verificación de DNI u otros casos donde necesitas enviar la imagen al servidor
     * @param uri Uri de la imagen
     * @param quality Calidad de compresión (0-100), default 85
     * @return String base64 o null si hay error
     */
    suspend fun uriToBase64(
        uri: Uri,
        quality: Int = 85
    ): String? = withContext(Dispatchers.IO) {
        val bitmap = uriToBitmap(uri) ?: return@withContext null
        bitmapToBase64(bitmap, quality)
    }
    
    /**
     * Crea una URL de data URI para imágenes base64
     * Formato: "data:image/jpeg;base64,{base64String}"
     * @param base64String String base64 de la imagen
     * @param mimeType Tipo MIME (default: "image/jpeg")
     * @return Data URI string
     */
    fun createDataUri(base64String: String, mimeType: String = "image/jpeg"): String {
        return "data:$mimeType;base64,$base64String"
    }
    
    /**
     * Procesa un Uri y retorna una data URI lista para enviar al servidor
     * @param uri Uri de la imagen
     * @param quality Calidad de compresión (0-100), default 85
     * @return Data URI string o null si hay error
     */
    suspend fun processImageForVerification(uri: Uri, quality: Int = 85): String? {
        val base64 = uriToBase64(uri, quality) ?: return null
        return createDataUri(base64)
    }
}

