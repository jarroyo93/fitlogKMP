package dev.josearroyo.fitlog

import android.os.Build
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

// 🔥 AGREGADO: Implementación nativa para la JVM/Android
actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

actual fun calcularFechaCierreCiclo(inicioMilis: Long): Long {
    val calendar = java.util.Calendar.getInstance().apply {
        timeInMillis = inicioMilis
        add(java.util.Calendar.DAY_OF_YEAR, 7)
        set(java.util.Calendar.HOUR_OF_DAY, 23)
        set(java.util.Calendar.MINUTE, 59)
        set(java.util.Calendar.SECOND, 59)
        set(java.util.Calendar.MILLISECOND, 999)
    }
    return calendar.timeInMillis
}

actual fun esMismoDia(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

actual fun formatearHora(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    return SimpleDateFormat("hh:mm a", Locale("es", "ES")).format(date)
}

actual fun formatearFechaHora(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    return SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale("es", "ES")).format(date)
}

actual fun esCumpleanosHoy(fechaNacimiento: Long): Boolean {
    val calHoy = java.util.Calendar.getInstance()
    val calNac = java.util.Calendar.getInstance().apply { timeInMillis = fechaNacimiento }
    return calHoy.get(java.util.Calendar.MONTH) == calNac.get(java.util.Calendar.MONTH) &&
            calHoy.get(java.util.Calendar.DAY_OF_MONTH) == calNac.get(java.util.Calendar.DAY_OF_MONTH)
}

actual fun formatearFechaHistorial(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val sdf = java.text.SimpleDateFormat("dd 'de' MMMM, yyyy", java.util.Locale("es", "ES"))
    return sdf.format(date)
}