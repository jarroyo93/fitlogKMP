package dev.josearroyo.fitlog.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth // 🔥 Importación para la gestión de credenciales compartida
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import dev.josearroyo.fitlog.data.model.Habitos
import dev.josearroyo.fitlog.data.model.PeriodoFacturable
import dev.josearroyo.fitlog.data.model.RolUsuario
import dev.josearroyo.fitlog.data.model.RutinaAsignada
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.data.model.ValoracionFisica
import dev.josearroyo.fitlog.getCurrentTimeMillis

class AtletaRepository {
    private val db = Firebase.firestore
    private val usersRef = db.collection("users")
    private val auth = Firebase.auth

    // Helper multiplataforma para auto-generar identificadores alfanuméricos únicos de Firestore (20 caracteres)
    private fun generarDocumentId(): String {
        return (1..20).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")
    }

    // ============================================================
    // BLOQUE 1: PERFIL DEL USUARIO
    // ============================================================

    suspend fun obtenerUsuario(atletaId: String): Usuario? = try {
        val doc = usersRef.document(atletaId).get()
        if (doc.exists) doc.data<Usuario>().copy(id = doc.id) else null
    } catch (e: Exception) { null }

    // ============================================================
    // BLOQUE 2: VALORACIONES FÍSICAS
    // ============================================================

    suspend fun obtenerUltimaValoracion(atletaId: String): ValoracionFisica? = try {
        val snapshot = usersRef.document(atletaId).collection("valoraciones")
            .orderBy("fechaRegistro", Direction.DESCENDING)
            .limit(1).get()

        snapshot.documents.firstOrNull()?.let { doc ->
            doc.data<ValoracionFisica>().copy(id = doc.id)
        }
    } catch (e: Exception) { null }

    suspend fun obtenerHistorialValoraciones(atletaId: String): List<ValoracionFisica> = try {
        val snapshot = usersRef.document(atletaId).collection("valoraciones")
            .orderBy("fechaRegistro", Direction.DESCENDING).get()

        snapshot.documents.map { doc ->
            doc.data<ValoracionFisica>().copy(id = doc.id)
        }
    } catch (e: Exception) { emptyList() }

    suspend fun guardarValoracion(atletaId: String, valoracion: ValoracionFisica): Boolean = try {
        // 🔥 Corrección: Se estampa la marca de tiempo usando getCurrentTimeMillis() en lugar de Date()
        val valoracionConFecha = valoracion.copy(fechaRegistro = getCurrentTimeMillis())
        usersRef.document(atletaId).collection("valoraciones").add(valoracionConFecha)
        true
    } catch (e: Exception) { false }

    // ============================================================
    // BLOQUE 3: HÁBITOS Y ESTILO DE VIDA
    // ============================================================

    suspend fun obtenerUltimosHabitos(atletaId: String): Habitos? = try {
        val snapshot = usersRef.document(atletaId).collection("habitos")
            .orderBy("fechaRegistro", Direction.DESCENDING)
            .limit(1).get()

        if (snapshot.documents.isNotEmpty()) {
            val doc = snapshot.documents[0]
            doc.data<Habitos>().copy(id = doc.id)
        } else null
    } catch (e: Exception) { null }

    suspend fun obtenerHistorialHabitos(atletaId: String): List<Habitos> = try {
        val snapshot = usersRef.document(atletaId).collection("habitos")
            .orderBy("fechaRegistro", Direction.DESCENDING).get()

        snapshot.documents.map { doc ->
            doc.data<Habitos>().copy(id = doc.id)
        }
    } catch (e: Exception) { emptyList() }

    suspend fun guardarHabitos(atletaId: String, habitos: Habitos): Boolean = try {
        // 🔥 Corrección: Uso de marca de tiempo pura Long
        val habitosConFecha = habitos.copy(fechaRegistro = getCurrentTimeMillis())
        usersRef.document(atletaId).collection("habitos").add(habitosConFecha)
        true
    } catch (e: Exception) { false }

    // ============================================================
    // BLOQUE 4: GESTIÓN DE RUTINAS (CLONES)
    // ============================================================

    suspend fun obtenerRutinasActivas(atletaId: String): List<RutinaAsignada> = try {
        val snapshot = usersRef.document(atletaId).collection("rutinas_asignadas")
            .where("estaActiva", equalTo = true)
            .get()

        snapshot.documents.map { doc ->
            doc.data<RutinaAsignada>().copy(id = doc.id)
        }
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun obtenerRutinaAsignada(atletaId: String, rutinaId: String): RutinaAsignada? = try {
        val doc = usersRef.document(atletaId).collection("rutinas_asignadas").document(rutinaId).get()
        if (doc.exists) {
            doc.data<RutinaAsignada>().copy(id = doc.id)
        } else null
    } catch (e: Exception) {
        null
    }

    suspend fun actualizarRutinaAsignada(atletaId: String, rutina: RutinaAsignada): Boolean = try {
        usersRef.document(atletaId).collection("rutinas_asignadas").document(rutina.id).set(rutina)
        true
    } catch (e: Exception) { false }

    suspend fun asignarRutina(atletaId: String, rutina: RutinaAsignada): Boolean = try {
        usersRef.document(atletaId).collection("rutinas_asignadas").add(rutina)
        true
    } catch (e: Exception) { false }

    suspend fun eliminarRutinaAsignada(atletaId: String, rutinaId: String): Boolean = try {
        usersRef.document(atletaId).collection("rutinas_asignadas").document(rutinaId).delete()
        true
    } catch (e: Exception) { false }

    // ============================================================
    // BLOQUE 5: CREACIÓN INICIAL DE RAÍZ (CON ENLACE CONTABLE GENERAL)
    // ============================================================

    suspend fun crearAtletaCompleto(
        usuario: Usuario,
        valoracion: ValoracionFisica,
        habitos: Habitos,
        contrasenaTemporal: String,
        primerPeriodo: PeriodoFacturable
    ): Boolean {
        try {
            val snapshotCorreo = db.collection("users")
                .where("correo", equalTo = usuario.correo)
                .get()

            if (snapshotCorreo.documents.isNotEmpty()) {
                return false
            }

            val snapshotDoc = db.collection("users")
                .where("numeroDocumento", equalTo = usuario.numeroDocumento)
                .where("rol", equalTo = "ATLETA")
                .get()

            if (snapshotDoc.documents.isNotEmpty()) {
                return false
            }

            // Invocación a Firebase Auth Multiplatform de GitLive
            val authResult = auth.createUserWithEmailAndPassword(usuario.correo, contrasenaTemporal)
            val authUid = authResult.user?.uid ?: return false

            val nuevoRef = usersRef.document(authUid)
            val ahoraMilis = getCurrentTimeMillis()

            // 🔥 RESOLUCIÓN DE LIMITANTES DE CONSTRUCTOR VACÍO EN GITLIVE:
            val idUnicoCompartido = generarDocumentId()
            val valId = generarDocumentId()
            val habId = generarDocumentId()

            val periodoRef = nuevoRef.collection("periodos_facturables").document(idUnicoCompartido)
            val registroContableRef = db.collection("historial_facturacion_general").document(idUnicoCompartido)

            // Orquestación del Batch transaccional en GitLive KMP
            val batch = db.batch()

            // 1. Guardar datos maestros del Atleta
            batch.set(nuevoRef, usuario.copy(
                id = authUid,
                authId = authUid,
                rol = RolUsuario.ATLETA
            ))

            // 2. Guardar Valoración Inicial
            val valRef = nuevoRef.collection("valoraciones").document(valId)
            batch.set(valRef, valoracion.copy(fechaRegistro = ahoraMilis))

            // 3. Guardar Hábitos Iniciales
            val habRef = nuevoRef.collection("habitos").document(habId)
            batch.set(habRef, habitos.copy(fechaRegistro = ahoraMilis))

            // 4. Guardar Periodo Inicial Operativo dentro del Atleta
            batch.set(periodoRef, primerPeriodo.copy(
                id = idUnicoCompartido,
                atletaId = authUid,
                entrenadorId = usuario.entrenadorId ?: ""
            ))

            // 5. Emitir recibo contable en la colección raíz general
            val reciboContableInicial = mapOf(
                "id" to idUnicoCompartido,
                "entrenadorId" to (usuario.entrenadorId ?: ""),
                "atletaId" to authUid,
                "atletaNombreSnapshot" to "${usuario.nombres} ${usuario.apellidos}".trim(),
                "tipoPlan" to primerPeriodo.tipoPlan,
                "fechaInicio" to primerPeriodo.fechaInicio,
                "fechaFin" to primerPeriodo.fechaFin,
                "fechaRegistroTransaccion" to ahoraMilis,
                "estado" to primerPeriodo.estado.name
            )
            batch.set(registroContableRef, reciboContableInicial)

            batch.commit()
            return true

        } catch (e: Exception) {
            return false
        }
    }
}