package com.example.getjob.presentation.screens.register

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { 
            Text(
                placeholder,
                fontSize = 14.sp,
                color = RegisterColors.PlaceholderGray
            ) 
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp), // Aumentar altura para que el texto no se corte por abajo
        singleLine = true,
        shape = RoundedCornerShape(30.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = RegisterColors.White,
            focusedContainerColor = RegisterColors.White,
            unfocusedBorderColor = RegisterColors.BorderGray,
            focusedBorderColor = RegisterColors.BorderGray,
            unfocusedTextColor = RegisterColors.DarkGray,
            focusedTextColor = RegisterColors.DarkGray, // Texto visible cuando está enfocado
            errorBorderColor = RegisterColors.BorderGray // Mantener borde gris incluso con error
        ),
        leadingIcon = {
            Icon(
                leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = RegisterColors.IconGray
            )
        },
        visualTransformation = visualTransformation,
        isError = isError,
        supportingText = supportingText?.let {
            {
                Text(
                    it,
                    color = if (isError) MaterialTheme.colorScheme.error else RegisterColors.PlaceholderGray,
                    fontSize = 12.sp
                )
            }
        }
    )
}

