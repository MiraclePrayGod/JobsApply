package com.example.getjob.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Sistema de eventos para manejar eventos de autenticación
 * Permite notificar cuando el token expira y se necesita redirigir al login
 */
object AuthEventBus {
    private val _authEvents = MutableSharedFlow<AuthEvent>(replay = 0)
    val authEvents: SharedFlow<AuthEvent> = _authEvents.asSharedFlow()
    
    /**
     * Emite un evento de token expirado
     */
    suspend fun emitTokenExpired() {
        _authEvents.emit(AuthEvent.TokenExpired)
    }
    
    /**
     * Emite un evento de logout
     */
    suspend fun emitLogout() {
        _authEvents.emit(AuthEvent.Logout)
    }
    
    sealed class AuthEvent {
        object TokenExpired : AuthEvent()
        object Logout : AuthEvent()
    }
}

