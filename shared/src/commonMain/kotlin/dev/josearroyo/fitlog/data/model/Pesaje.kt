package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Pesaje(
    val id: String = "", // Removido @DocumentId
    val pesoKg: Double = 0.0,
    val fecha: Long = 0L, // Cambiado Date -> Long
    val notas: String = ""
)