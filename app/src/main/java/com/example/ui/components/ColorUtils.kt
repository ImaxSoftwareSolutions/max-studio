package com.example.ui.components

import androidx.compose.ui.graphics.Color

fun parseHexColor(hex: String, fallback: Color = Color.Transparent): Color {
    if (hex.equals("#00000000", ignoreCase = true) || hex.isBlank()) {
        return fallback
    }
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

val StudioSwatchPalette = listOf(
    "#22D3EE",
    "#6366F1",
    "#10B981",
    "#F59E0B",
    "#F43F5E",
    "#8B5CF6",
    "#F8FAFC",
    "#1E293B",
    "#0B1120",
    "#00000000",
)
