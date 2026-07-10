package dev.josearroyo.fitlog.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore

class AuthRepository {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun login(email: String, clave: String): String {
        return try {
            // En GitLive, las llamadas ya son suspendidas, no llevan .await()
            val result = auth.signInWithEmailAndPassword(email, clave)
            result.user?.uid ?: throw Exception("Error al obtener el UID de Firebase")
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun logout() {
        auth.signOut()
    }

    // =========================================================
    // CORRECCI DE RA Z: Cambio de contrase a en primer ingreso
    // =========================================================
    suspend fun cambiarContrasenaPrimeraVez(uid: String, nuevaContrasena: String): Result<Boolean> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No hay una sesión activa."))

            // Actualizamos en Firebase Auth
            user.updatePassword(nuevaContrasena)

            // Apagamos el flag en Firestore de forma limpia usando sintaxis KMP
            db.collection("users").document(uid)
                .update("requiereCambioContrasena" to false)

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}