package dev.josearroyo.fitlog.data.model

enum class EstadoSuscripcion { ACTIVO, SUSPENDIDO, VENCIDO, HUERFANO }
enum class RolUsuario { SUPERADMIN, ENTRENADOR, ATLETA }

enum class TipoPlanSuscripcion(val dias: Int, val etiqueta: String) {
    SEMANAL(7, "Semanal (7 días)"),
    QUINCENAL(15, "Quincenal (15 días)"),
    MENSUAL(30, "Mensual (30 días)"),
    TRIMESTRAL(90, "Trimestral (90 días)"),
    PERSONALIZADO(0, "Personalizado")
}

enum class EstadoPeriodo { ACTIVO, DIFERIDO, COMPLETADO, CANCELADO, CONGELADO }

data class PeriodoFacturable(
    val id: String = "", // Removido @DocumentId
    val entrenadorId: String = "",
    val atletaId: String = "",
    val tipoPlan: String = "",
    val fechaInicio: Long = 0L,
    val fechaFin: Long? = null,
    val fechaCreacion: Long = 0L,
    val estado: EstadoPeriodo = EstadoPeriodo.DIFERIDO,
    val diasRestantesAlCongelar: Long = 0
)

data class Usuario(
    val id: String = "", // Removido @DocumentId
    val rol: RolUsuario = RolUsuario.ATLETA,
    val entrenadorId: String? = null,
    val estadoSuscripcion: EstadoSuscripcion = EstadoSuscripcion.HUERFANO,
    val codigoVinculacion: String? = null,
    val expiracionCodigo: Long? = null,
    val authId: String = "",
    val fechaVencimientoAnualidad: Long? = null,
    val nombres: String = "",
    val apellidos: String = "",
    val tipoDocumento: String = "",
    val numeroDocumento: String = "",
    val fechaNacimiento: Long = 0L, // Cambiado Date -> Long
    val tipoSangre: String = "",
    val nacionalidad: String = "",
    val telefono: String = "",
    val planActivo: String = "Ninguno",
    val fechaInicioSuscripcion: Long? = null,
    val vencimientoSuscripcion: Long? = null,
    val correo: String = "",
    val fechaCreacion: Long = 0L, // Cambiado Date -> Long
    val saldoMilisegundosRestantes: Long? = null,
    val motivoPausa: String? = null,
    val requiereCambioContrasena: Boolean = false,
    val fotoPerfilUrl: String? = null,
    val especialidad: String? = null,
    val biografia: String? = null,
    val certificaciones: List<String> = listOf()
)