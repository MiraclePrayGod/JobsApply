package com.example.getjob.presentation.screens.jobflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.getjob.presentation.viewmodel.JobStage
import com.example.getjob.presentation.screens.register.RegisterColors

@Composable
fun JobFlowActionBar(
    stage: JobStage,
    onPrimaryAction: () -> Unit,
    onChat: () -> Unit,
    isPrimaryActionLoading: Boolean
) {
    val primaryButtonText = when (stage) {
        JobStage.ACCEPTED -> "Iniciar ruta"
        JobStage.ON_ROUTE -> "Confirmar llegada"
        JobStage.ARRIVED -> "Iniciar trabajo"
        JobStage.IN_SERVICE -> "Finalizar servicio"
        JobStage.COMPLETED -> "Finalizar y enviar" // No debería mostrarse
        JobStage.REVIEWED -> "Volver al inicio"
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Botón Chat (siempre visible excepto en REVIEWED)
        if (stage != JobStage.REVIEWED) {
            OutlinedButton(
                onClick = onChat,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = RegisterColors.White,
                    contentColor = RegisterColors.DarkGray
                ),
                border = BorderStroke(1.dp, RegisterColors.BorderGray)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Chat")
            }
        }
        
        // Botón principal de acción
        Button(
            onClick = onPrimaryAction,
            modifier = Modifier.weight(1f),
            enabled = !isPrimaryActionLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = RegisterColors.PrimaryOrange,
                contentColor = RegisterColors.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isPrimaryActionLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = RegisterColors.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(primaryButtonText)
            }
        }
    }
}

