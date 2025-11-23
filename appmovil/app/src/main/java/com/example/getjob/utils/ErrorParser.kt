package com.example.getjob.utils

import com.google.gson.JsonParser
import java.net.SocketTimeoutException
import java.net.ConnectException
import java.net.UnknownHostException
import java.io.IOException

object ErrorParser {
    /**
     * Parsea errores de red y devuelve mensajes amigables para el usuario
     */
    fun parseNetworkError(exception: Throwable): String {
        return when (exception) {
            is SocketTimeoutException -> {
                "Tiempo de espera agotado. Por favor verifica tu conexión a internet e intenta nuevamente."
            }
            is ConnectException -> {
                "No se pudo conectar al servidor. Por favor verifica tu conexión a internet."
            }
            is UnknownHostException -> {
                "No se pudo conectar al servidor. Por favor verifica tu conexión a internet."
            }
            is IOException -> {
                "Error de conexión. Por favor verifica tu conexión a internet e intenta nuevamente."
            }
            else -> {
                exception.message ?: "Error de conexión. Por favor intenta nuevamente."
            }
        }
    }
    
    /**
     * Parsea errores de FastAPI y devuelve mensajes amigables para el usuario
     */
    fun parseFastApiError(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) {
            return "Error desconocido"
        }
        
        return try {
            val json = JsonParser.parseString(errorBody).asJsonObject
            
            // FastAPI devuelve errores en formato: {"detail": [...]}
            if (json.has("detail")) {
                val detail = json.get("detail")
                
                // Si detail es un array
                if (detail.isJsonArray) {
                    val errors = detail.asJsonArray
                    if (errors.size() > 0) {
                        val firstError = errors[0].asJsonObject
                        val msg = firstError.get("msg")?.asString
                        
                        // Traducir mensajes comunes a español
                        when {
                            msg?.contains("email", ignoreCase = true) == true -> {
                                if (msg.contains("@-sign", ignoreCase = true) || 
                                    msg.contains("@", ignoreCase = true)) {
                                    "Por favor ingresa un correo electrónico válido"
                                } else if (msg.contains("already registered", ignoreCase = true) ||
                                          msg.contains("ya está registrado", ignoreCase = true)) {
                                    "Este correo electrónico ya está registrado"
                                } else {
                                    "El correo electrónico no es válido"
                                }
                            }
                            msg?.contains("password", ignoreCase = true) == true -> {
                                "La contraseña no es válida"
                            }
                            msg?.contains("required", ignoreCase = true) == true -> {
                                "Por favor completa todos los campos requeridos"
                            }
                            else -> {
                                // Extraer mensaje y hacerlo más amigable
                                msg?.replace("value is not a valid", "No es válido")
                                    ?.replace("An email address must have", "El correo debe tener")
                                    ?.replace("unable to parse", "no se pudo procesar")
                                    ?: "Error de validación. Por favor verifica los datos ingresados."
                            }
                        }
                    } else {
                        "Error de validación"
                    }
                } 
                // Si detail es un string simple
                else if (detail.isJsonPrimitive) {
                    detail.asString
                } else {
                    "Error de validación"
                }
            } else {
                // Si no tiene el formato esperado, devolver un mensaje genérico
                "Error al procesar la solicitud. Por favor verifica los datos ingresados."
            }
        } catch (e: Exception) {
            // Si falla el parsing, devolver un mensaje genérico
            "Error al procesar la solicitud. Por favor verifica los datos ingresados."
        }
    }
    
    /**
     * Parsea errores HTTP y devuelve mensajes amigables para el usuario
     * Maneja tanto errores de red como errores del servidor
     */
    fun parseError(exception: Throwable, errorBody: String? = null): String {
        // Verificar si es un error de red conocido
        val isNetworkError = exception is SocketTimeoutException ||
                exception is ConnectException ||
                exception is UnknownHostException ||
                exception is IOException
        
        // Si es un error de red, usar parseNetworkError
        if (isNetworkError) {
            return parseNetworkError(exception)
        }
        
        // Si hay errorBody, intentar parsearlo como error de FastAPI
        if (errorBody != null) {
            return parseFastApiError(errorBody)
        }
        
        // Si no hay errorBody y no es error de red, devolver mensaje genérico o el mensaje de la excepción
        return exception.message ?: "Error desconocido. Por favor intenta nuevamente."
    }
}

