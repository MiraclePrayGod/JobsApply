package com.example.getjob.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.util.Base64

class ImageStorageManager(private val context: Context) {
    private val imageDir: File by lazy {
        File(context.filesDir, "chat_images").apply {
            if (!exists()) mkdirs()
        }
    }
    
    private val IMAGE_EXPIRY_HOURS = 10L
    private val MILLIS_PER_HOUR = 60 * 60 * 1000L
    
    /**
     * Guarda una imagen en base64 y retorna el nombre del archivo
     */
    suspend fun saveImage(base64Image: String, messageId: Int): String? = withContext(Dispatchers.IO) {
        try {
            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
            val fileName = "msg_${messageId}_${System.currentTimeMillis()}.jpg"
            val file = File(imageDir, fileName)
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            
            Log.d("ImageStorage", "Imagen guardada: ${file.absolutePath}")
            fileName
        } catch (e: Exception) {
            Log.e("ImageStorage", "Error al guardar imagen", e)
            null
        }
    }
    
    /**
     * Obtiene una imagen guardada
     */
    suspend fun getImage(fileName: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(imageDir, fileName)
            if (file.exists() && !isImageExpired(file)) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                if (file.exists()) {
                    file.delete() // Eliminar si está expirada
                }
                null
            }
        } catch (e: Exception) {
            Log.e("ImageStorage", "Error al obtener imagen", e)
            null
        }
    }
    
    /**
     * Verifica si una imagen ha expirado (10 horas)
     */
    private fun isImageExpired(file: File): Boolean {
        val fileAge = System.currentTimeMillis() - file.lastModified()
        return fileAge > (IMAGE_EXPIRY_HOURS * MILLIS_PER_HOUR)
    }
    
    /**
     * Limpia imágenes expiradas
     */
    suspend fun cleanExpiredImages() = withContext(Dispatchers.IO) {
        try {
            val files = imageDir.listFiles()
            files?.forEach { file ->
                if (isImageExpired(file)) {
                    file.delete()
                    Log.d("ImageStorage", "Imagen expirada eliminada: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("ImageStorage", "Error al limpiar imágenes", e)
        }
    }
    
    /**
     * Elimina una imagen específica
     */
    suspend fun deleteImage(fileName: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(imageDir, fileName)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("ImageStorage", "Error al eliminar imagen", e)
        }
    }
    
    /**
     * Guarda la foto de perfil del usuario (sin expiración)
     */
    suspend fun saveProfileImage(bitmap: Bitmap, userId: Int): String? = withContext(Dispatchers.IO) {
        try {
            val profileImageDir = File(context.filesDir, "profile_images").apply {
                if (!exists()) mkdirs()
            }
            
            val fileName = "profile_${userId}.jpg"
            val file = File(profileImageDir, fileName)
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            Log.d("ImageStorage", "Foto de perfil guardada: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e("ImageStorage", "Error al guardar foto de perfil", e)
            null
        }
    }
    
    /**
     * Obtiene la foto de perfil del usuario
     */
    suspend fun getProfileImage(userId: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val profileImageDir = File(context.filesDir, "profile_images")
            val file = File(profileImageDir, "profile_${userId}.jpg")
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ImageStorage", "Error al obtener foto de perfil", e)
            null
        }
    }
    
    /**
     * Obtiene la ruta de la foto de perfil del usuario
     */
    fun getProfileImagePath(userId: Int): String? {
        val profileImageDir = File(context.filesDir, "profile_images")
        val file = File(profileImageDir, "profile_${userId}.jpg")
        return if (file.exists()) file.absolutePath else null
    }
}

