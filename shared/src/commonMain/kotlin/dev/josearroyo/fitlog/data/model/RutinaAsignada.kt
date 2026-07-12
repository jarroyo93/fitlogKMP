package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RutinaAsignada(
    var id: String = "",
    val nombreRutina: String = "",
    val fechaAsignacion: Long = 0L, // Cambiado Date -> Long
    val estaActiva: Boolean = true,
    val notasEntrenador: String = "",
    val diasEntrenamiento: List<DiaEntrenamientoAsignado> = emptyList(),
    val ultimaVezEjecutada: Long? = null // Cambiado Date? -> Long?
)

@Serializable
data class DiaEntrenamientoAsignado(
    val idDia: String = "", // Cambiado UUID -> String vacío
    val plantillaOriginalId: String = "",
    val nombreDia: String = "",
    val ordenSecuencia: Int = 0,
    val ejercicios: List<EjercicioAsignado> = emptyList(),
    val ultimaVezEjecutada: Long? = null // Cambiado Date? -> Long?
)

@Serializable
data class EjercicioAsignado(
    val idInterno: String = "", // Cambiado UUID -> String vacío
    val ejercicioGlobalId: String = "",
    val nombre: String = "",
    val seriesPrescritas: List<PrescripcionSerie> = emptyList(),
    val descansoSegundos: Int = 0,
    val notasEspecificas: String = "",
    val ordenSecuencia: Int = 0
)