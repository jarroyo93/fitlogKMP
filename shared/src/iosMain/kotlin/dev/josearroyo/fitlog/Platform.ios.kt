package dev.josearroyo.fitlog

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.UIKit.UIDevice
// 🔥 IMPORTS OBLIGATORIOS PARA EL MANEJO DE TIEMPO EN APPLE NATIVO:
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localeWithLocaleIdentifier
import platform.Foundation.timeIntervalSince1970

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

// 🔥 AGREGADO: Implementación nativa para el ecosistema Apple (Objective-C/Swift Runtime)
actual fun getCurrentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

actual fun calcularFechaCierreCiclo(inicioMilis: Long): Long {
    val calendar = platform.Foundation.NSCalendar.currentCalendar
    val date = platform.Foundation.NSDate.dateWithTimeIntervalSince1970(inicioMilis / 1000.0)
    val datePlusSeven = calendar.dateByAddingUnit(
        platform.Foundation.NSCalendarUnitDay,
        value = 7,
        toDate = date,
        options = 0UL
    ) ?: date
    return calendar.dateBySettingHour(23, minute = 59, second = 59, ofDate = datePlusSeven, options = 0UL)
        ?.timeIntervalSince1970?.times(1000)?.toLong() ?: (inicioMilis + 604800000L)
}

actual fun esMismoDia(timestamp1: Long, timestamp2: Long): Boolean {
    val calendar = NSCalendar.currentCalendar
    val date1 = NSDate.dateWithTimeIntervalSince1970(timestamp1 / 1000.0)
    val date2 = NSDate.dateWithTimeIntervalSince1970(timestamp2 / 1000.0)

    val comp1 = calendar.components(NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, fromDate = date1)
    val comp2 = calendar.components(NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, fromDate = date2)

    return comp1.year == comp2.year && comp1.month == comp2.month && comp1.day == comp2.day
}

actual fun formatearHora(timestamp: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "hh:mm a"
        locale = NSLocale(localeIdentifier = "es_ES")
    }
    return formatter.stringFromDate(date)
}

actual fun formatearFechaHora(timestamp: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "dd/MM/yyyy hh:mm a"
        locale = NSLocale(localeIdentifier = "es_ES")
    }
    return formatter.stringFromDate(date)
}

actual fun esCumpleanosHoy(fechaNacimiento: Long): Boolean {
    val calendar = platform.Foundation.NSCalendar.currentCalendar
    val hoy = platform.Foundation.NSDate()
    val nac = platform.Foundation.NSDate.dateWithTimeIntervalSince1970(fechaNacimiento / 1000.0)

    val compHoy = calendar.components(platform.Foundation.NSCalendarUnitMonth or platform.Foundation.NSCalendarUnitDay, fromDate = hoy)
    val compNac = calendar.components(platform.Foundation.NSCalendarUnitMonth or platform.Foundation.NSCalendarUnitDay, fromDate = nac)

    return compHoy.month == compNac.month && compHoy.day == compNac.day
}

actual fun formatearFechaHistorial(timestamp: Long): String {
    val date = platform.Foundation.NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val formatter = platform.Foundation.NSDateFormatter().apply {
        dateFormat = "dd 'de' MMMM, yyyy"
        locale = platform.Foundation.NSLocale.localeWithLocaleIdentifier("es_ES")
    }
    return formatter.stringFromDate(date)
}