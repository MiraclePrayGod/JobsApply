package com.example.getjob.utils

import android.content.Context
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat

/**
 * Servicio para notificar cuando el trabajador está cerca del destino
 */
class ProximityNotifier(private val context: Context) {
    private val vibrator: Vibrator? by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    private val notificationSound by lazy {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }
    
    private var hasNotified = false
    private val proximityRadius = 100f // metros - distancia para considerar "cerca"
    
    /**
     * Verifica si está cerca del destino y notifica si es necesario
     * @param currentLat Latitud actual
     * @param currentLon Longitud actual
     * @param destLat Latitud del destino
     * @param destLon Longitud del destino
     * @return true si está cerca del destino
     */
    fun checkProximity(
        currentLat: Double,
        currentLon: Double,
        destLat: Double,
        destLon: Double
    ): Boolean {
        val distance = LocationService.getInstance(context).calculateDistance(
            currentLat,
            currentLon,
            destLat,
            destLon
        )
        
        val isNear = distance <= proximityRadius
        
        if (isNear && !hasNotified) {
            notifyArrival()
            hasNotified = true
        } else if (!isNear) {
            // Resetear si se aleja del destino
            hasNotified = false
        }
        
        return isNear
    }
    
    /**
     * Notifica la llegada con sonido y vibración
     */
    private fun notifyArrival() {
        // Vibración
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 200, 100, 200),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(500)
        }
        
        // Sonido (opcional - puede ser molesto)
        // val ringtone = RingtoneManager.getRingtone(context, notificationSound)
        // ringtone?.play()
    }
    
    /**
     * Resetea el estado de notificación
     */
    fun reset() {
        hasNotified = false
    }
}

