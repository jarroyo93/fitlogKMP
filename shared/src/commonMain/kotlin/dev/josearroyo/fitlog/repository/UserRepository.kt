package dev.josearroyo.fitlog.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.josearroyo.fitlog.data.model.Usuario

class UserRepository {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    suspend fun obtenerUsuario(uid: String): Usuario? = try {
        val documentSnapshot = usersCollection.document(uid).get()
        if (documentSnapshot.exists) {
            // Mapeo multiplataforma nativo usando kotlinx.serialization
            documentSnapshot.data<Usuario>().copy(id = documentSnapshot.id)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }

    suspend fun guardarUsuario(usuario: Usuario): Boolean = try {
        usersCollection.document(usuario.id).set(usuario)
        true
    } catch (e: Exception) {
        false
    }
}