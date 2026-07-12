package dev.josearroyo.fitlog

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

// 🔥 AGREGA ESTA LÍNEA AL FINAL:
expect fun getCurrentTimeMillis(): Long
expect fun calcularFechaCierreCiclo(inicioMilis: Long): Long
expect fun esMismoDia(timestamp1: Long, timestamp2: Long): Boolean
expect fun formatearHora(timestamp: Long): String