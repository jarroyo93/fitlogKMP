package dev.josearroyo.fitlog.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import dev.josearroyo.fitlog.data.model.Ejercicio
import dev.josearroyo.fitlog.data.model.PlantillaRutina

class ExerciseRepository {
    private val db = Firebase.firestore
    private val globalExercisesRef = db.collection("biblioteca_global")
    private val customExercisesRef = db.collection("ejercicios_personalizados")
    private val templatesRef = db.collection("plantillas_rutinas")

    // ==========================================
    // BLOQUE 1: EJERCICIOS (PERSONALIZADOS Y GLOBAL)
    // ==========================================
    suspend fun obtenerBibliotecaCompleta(entrenadorId: String): List<Ejercicio> = try {
        // En KMP deserializamos usando el método genérico .data<T>()
        val globales = globalExercisesRef.get().documents.map { it.data<Ejercicio>() }

        // 🔥 Corrección: Se usa .where("campo", equalTo = valor)
        val personalizadosSnapshot = customExercisesRef.where("creadorId", equalTo = entrenadorId).get()
        val listaPersonalizados = personalizadosSnapshot.documents.map { doc ->
            doc.data<Ejercicio>().copy(id = doc.id)
        }

        (globales + listaPersonalizados).filter { it.activo }
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun obtenerEjercicioPorId(ejercicioId: String): Ejercicio? = try {
        val doc = customExercisesRef.document(ejercicioId).get()
        if (doc.exists) doc.data<Ejercicio>().copy(id = doc.id) else null
    } catch (e: Exception) {
        null
    }

    suspend fun guardarEjercicioPersonalizado(ejercicio: Ejercicio): Boolean = try {
        customExercisesRef.add(ejercicio)
        true
    } catch (e: Exception) {
        false
    }

    suspend fun actualizarEjercicioPersonalizado(ejercicioId: String, datos: Map<String, Any?>): Boolean = try {
        // Convertimos el mapa a vararg de Pairs requerido por GitLive
        val pairs = datos.map { it.key to it.value }.toTypedArray()
        customExercisesRef.document(ejercicioId).update(*pairs)
        true
    } catch (e: Exception) {
        false
    }

    suspend fun eliminarEjercicioFisico(ejercicioId: String): Boolean = try {
        customExercisesRef.document(ejercicioId).delete()
        true
    } catch (e: Exception) {
        false
    }

    // ==========================================
    // BLOQUE 2: PLANTILLAS (BIBLIOTECA DEL ENTRENADOR)
    // ==========================================
    suspend fun obtenerPlantillasDelEntrenador(entrenadorId: String): List<PlantillaRutina> = try {
        // 🔥 Corrección: Se usa .where("campo", equalTo = valor)
        val snapshot = templatesRef.where("entrenadorId", equalTo = entrenadorId).get()
        snapshot.documents.map { doc ->
            doc.data<PlantillaRutina>().copy(id = doc.id)
        }.filter { it.activo }
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun obtenerPlantillaPorId(id: String): PlantillaRutina? = try {
        val doc = templatesRef.document(id).get()
        if (doc.exists) doc.data<PlantillaRutina>().copy(id = doc.id) else null
    } catch (e: Exception) {
        null
    }
}