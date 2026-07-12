package dev.josearroyo.fitlog

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getCurrentTimeMillis(): Long
expect fun calcularFechaCierreCiclo(inicioMilis: Long): Long
expect fun esMismoDia(timestamp1: Long, timestamp2: Long): Boolean
expect fun formatearHora(timestamp: Long): String
expect fun formatearFechaHora(timestamp: Long): String
expect fun esCumpleanosHoy(fechaNacimiento: Long): Boolean