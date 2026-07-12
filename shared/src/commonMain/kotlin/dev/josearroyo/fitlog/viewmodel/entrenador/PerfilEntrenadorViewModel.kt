package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.Usuario // 🔥 Importación al nuevo paquete de modelos
import dev.josearroyo.fitlog.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Modificado el nombre para alinearlo al contexto del Entrenador
data class PerfilEntrenadorUiState(
    val usuarioLogueado: Usuario? = null,
    val entrenadorAsignado: Usuario? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val exitoGuardado: Boolean = false
)

class PerfilEntrenadorViewModel : ViewModel() {
    private val userRepository = UserRepository()

    private val _uiState = MutableStateFlow(PerfilEntrenadorUiState())
    val uiState: StateFlow<PerfilEntrenadorUiState> = _uiState.asStateFlow()

    fun cargarPerfil(uid: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val user = userRepository.obtenerUsuario(uid)
            if (user != null) {
                _uiState.value = _uiState.value.copy(usuarioLogueado = user, isLoading = false)
                if (user.entrenadorId != null) {
                    cargarEntrenadorAsignado(user.entrenadorId)
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "No se encontró el perfil.")
            }
        }
    }

    private fun cargarEntrenadorAsignado(entrenadorId: String) {
        viewModelScope.launch {
            val entrenador = userRepository.obtenerUsuario(entrenadorId)
            _uiState.value = _uiState.value.copy(entrenadorAsignado = entrenador)
        }
    }

    fun guardarPerfilEntrenador(uid: String, especialidad: String, biografia: String, certificaciones: List<String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, exitoGuardado = false)
            val campos = mapOf(
                "especialidad" to especialidad,
                "biografia" to biografia,
                "certificaciones" to certificaciones
            )
            val exito = userRepository.actualizarPerfilUsuario(uid, campos)
            if (exito) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    exitoGuardado = true,
                    usuarioLogueado = _uiState.value.usuarioLogueado?.copy(
                        especialidad = especialidad,
                        biografia = biografia,
                        certificaciones = certificaciones
                    )
                )
            } else {
                _uiState.value = _uiState.value.copy(isSaving = false, error = "Error al actualizar base de datos.")
            }
        }
    }

    fun guardarDatosPersonales(uid: String, nombres: String, apellidos: String, documento: String, telefono: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, exitoGuardado = false, error = null)
            val exito = userRepository.actualizarDatosPersonales(
                uid = uid,
                nombres = nombres,
                apellidos = apellidos,
                documento = documento,
                telefono = telefono
            )
            if (exito) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    exitoGuardado = true,
                    usuarioLogueado = _uiState.value.usuarioLogueado?.copy(
                        nombres = nombres,
                        apellidos = apellidos,
                        numeroDocumento = documento,
                        telefono = telefono
                    )
                )
            } else {
                _uiState.value = _uiState.value.copy(isSaving = false, error = "Error al actualizar los datos personales.")
            }
        }
    }

    fun resetExito() {
        _uiState.value = _uiState.value.copy(exitoGuardado = false)
    }
}