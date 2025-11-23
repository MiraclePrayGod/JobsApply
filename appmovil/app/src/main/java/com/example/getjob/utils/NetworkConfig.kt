package com.example.getjob.utils

object NetworkConfig {
    // URL base del backend
    // 
    // Para desarrollo local con ngrok:
    // 1. Levanta tu backend: uvicorn app.main:app --reload --port 8000
    // 2. En otra terminal: ngrok http 8000
    // 3. Copia la URL HTTPS de ngrok (ej: https://abc123.ngrok-free.app)
    // 4. Reemplaza la URL abajo con tu URL de ngrok
    //
    // Para producción: usar URL de Railway/Render
    
    // Para desarrollo local con Laragon:
    // - Emulador Android: usar "http://10.0.2.2:8000"
    // - Dispositivo físico (misma WiFi): usar tu IP local (ej: "http://192.168.1.100:8000")
    // Para encontrar tu IP: ejecuta "ipconfig" en PowerShell y busca "IPv4 Address"
    
    // ⚠️ IMPORTANTE: Para acceder desde otro celular (internet), usa la URL de ngrok
    // 1. Ejecuta: ngrok http 8000
    // 2. Copia la URL HTTPS que aparece (ej: https://abc123.ngrok-free.app)
    // 3. Reemplaza la URL abajo con tu URL de ngrok
    
    // Para desarrollo con ngrok (acceso desde cualquier dispositivo):
    // ⚠️ IMPORTANTE: Retrofit requiere que BASE_URL termine en /
    const val BASE_URL = "https://ballistic-amara-unjacketed.ngrok-free.dev/"
    
    // Para desarrollo local (emulador) - descomenta si solo usas emulador:
    // const val BASE_URL = "http://10.0.2.2:8000/"
    
    // Si usas dispositivo físico en la misma WiFi - descomenta y cambia por tu IP local:
    // const val BASE_URL = "http://192.168.1.100:8000/"
    
    // Para producción: "https://tu-app.railway.app"
}

