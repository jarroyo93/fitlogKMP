package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable


@Serializable
data class CicloEntrenamiento(
    val id: String = "", // 👈 Recomendación: Inicializar vacío y usar el ID autogenerado de Firebase
    val atletaId: String = "",
    val rutinaAsignadaId: String = "",

    // --- CONTROL DE TIEMPO (Cambiado a Long para ser Multiplataforma) ---
    val fechaInicio: Long = 0L,  // Timestamp en milisegundos (Ej: 1718023200000)
    val fechaCierre: Long = 0L,  // Lo manejas sumándole los milisegundos de 7 días
    val estaActivo: Boolean = true,

    // --- MÉTRICA 1: ASISTENCIA ---
    val metaSesionesAsignadas: Int = 0,
    val sesionesCompletadas: Int = 0,
    val porcentajeAsistencia: Double = 0.0,

    // --- MÉTRICA 2: VOLUMEN ---
    val repeticionesMetaTotal: Int = 0,
    val repeticionesLogradasTotal: Int = 0,
    val porcentajeVolumenGlobal: Double = 0.0
)