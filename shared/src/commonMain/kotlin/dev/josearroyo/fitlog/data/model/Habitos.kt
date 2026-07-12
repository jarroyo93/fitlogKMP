package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Habitos(
    var id: String = "",
    val actividadesPrincipales: String = "",
    val horaDespertar: String = "",
    val horaDormir: String = "",
    val horasSueno: Double = 0.0,
    val diasDisponibles: String = "",
    val horarioEntrenamiento: String = "",
    val tiempoDisponibleMinutos: Int = 0,
    val fechaRegistro: Long = 0L // Cambiado Date -> Long
)