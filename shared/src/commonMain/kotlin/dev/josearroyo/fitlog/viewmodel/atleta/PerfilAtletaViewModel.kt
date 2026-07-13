package dev.josearroyo.fitlog.viewmodel.atleta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.data.model.RutinaAsignada // 🚀 Importación del modelo vinculada
import dev.josearroyo.fitlog.repository.AtletaRepository
import dev.josearroyo.fitlog.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PerfilAtletaState(
    val usuarioLogueado: Usuario? = null,
    val entrenadorAsignado: Usuario? = null,
    val rutinaActiva: RutinaAsignada? = null, // 🚀 NUEVO: Almacena la rutina para la UI
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val guardadoExitoso: Boolean = false
)

class PerfilAtletaViewModel : ViewModel() {
    private val atletaRepository = AtletaRepository()
    private val userRepository = UserRepository()
    private val auth = Firebase.auth

    private val _uiState = MutableStateFlow(PerfilAtletaState())
    val uiState: StateFlow<PerfilAtletaState> = _uiState.asStateFlow()

    fun cargarPerfil(atletaId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val atleta = atletaRepository.obtenerUsuario(atletaId)

            if (atleta != null) {
                // 1. Cargamos datos del coach de forma segura
                val coach = if (!atleta.entrenadorId.isNullOrBlank()) {
                    userRepository.obtenerUsuario(atleta.entrenadorId)
                } else null

                // 2. 🚀 EXTRACCIÓN DE PLANIFICACIÓN: Consultamos la subcolección de rutinas
                val rutinas = atletaRepository.obtenerRutinasActivas(atletaId)
                val rutinaActiva = rutinas.firstOrNull { it.estaActiva }

                // 3. Sincronizamos todo el estado de golpe
                _uiState.update { state ->
                    state.copy(
                        usuarioLogueado = atleta,
                        entrenadorAsignado = coach,
                        rutinaActiva = rutinaActiva, // 🚀 Inyectado
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "No se pudo cargar el perfil.") }
            }
        }
    }

    fun guardarPerfilAtleta(uid: String, nombres: String, apellidos: String, nuevoCorreo: String, nacionalidad: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val correoLimpio = nuevoCorreo.trim()
                val correoActual = _uiState.value.usuarioLogueado?.correo ?: ""

                if (correoLimpio != correoActual) {
                    val correoDuplicado = userRepository.existeCorreo(correoLimpio)
                    if (correoDuplicado) {
                        _uiState.update {
                            it.copy(isSaving = false, error = "El correo electrónico ya se encuentra registrado por otro usuario.")
                        }
                        return@launch
                    }
                }

                val userAuth = auth.currentUser
                if (userAuth != null && userAuth.email != correoLimpio) {
                    try {
                        userAuth.updateEmail(correoLimpio)
                    } catch (e: Exception) {
                        if (e.message?.contains("RECENT_LOGIN_REQUIRED", ignoreCase = true) == true ||
                            e.message?.contains("requires recent authentication", ignoreCase = true) == true) {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    error = "Por seguridad, cambiar tu correo requiere que hayas iniciado sesión recientemente. Cierra sesión e ingresa de nuevo."
                                )
                            }
                            return@launch
                        }
                        throw e
                    }
                }

                val campos = mapOf(
                    "nombres" to nombres.trim(),
                    "apellidos" to apellidos.trim(),
                    "correo" to correoLimpio,
                    "nacionalidad" to nacionalidad.trim()
                )

                val exitoFirestore = userRepository.actualizarPerfilUsuario(uid, campos)
                if (!exitoFirestore) {
                    throw Exception("No se pudieron guardar los cambios en el servidor. Inténtalo de nuevo.")
                }

                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        error = null,
                        guardadoExitoso = true,
                        usuarioLogueado = state.usuarioLogueado?.copy(
                            nombres = nombres.trim(),
                            apellidos = apellidos.trim(),
                            correo = correoLimpio,
                            nacionalidad = nacionalidad.trim()
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = e.message ?: "Ocurrió un error inesperado.")
                }
            }
        }
    }
}