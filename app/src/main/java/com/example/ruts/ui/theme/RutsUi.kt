package com.example.ruts.ui.theme

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object RutsUi {
    val primaryButtonColors
        @Composable get() = ButtonDefaults.buttonColors(
            containerColor = White,
            contentColor = Black,
            disabledContainerColor = SurfaceCard,
            disabledContentColor = TextSecondary,
        )

    val outlinedButtonColors
        @Composable get() = ButtonDefaults.outlinedButtonColors(
            contentColor = White,
            disabledContentColor = TextSecondary,
        )

    val textFieldColors
        @Composable get() = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            disabledTextColor = TextSecondary,
            cursorColor = White,
            focusedContainerColor = SurfaceCard,
            unfocusedContainerColor = SurfaceCard,
            disabledContainerColor = SurfaceCard,
            focusedBorderColor = Border,
            unfocusedBorderColor = Border,
            disabledBorderColor = Border,
            focusedLeadingIconColor = White,
            unfocusedLeadingIconColor = TextSecondary,
            focusedTrailingIconColor = White,
            unfocusedTrailingIconColor = TextSecondary,
            focusedPlaceholderColor = TextSecondary,
            unfocusedPlaceholderColor = TextSecondary,
        )
}
