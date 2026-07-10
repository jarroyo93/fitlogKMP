package dev.josearroyo.fitlog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.RolUsuario
import dev.josearroyo.fitlog.repository.AuthRepository
import dev.josearroyo.fitlog.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado de Login
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val uid: String, val rol: RolUsuario) : AuthState()
    data class Error(val message: String) : AuthState()
}

// Estado separado para la pantalla de Activación / Primer Ingreso
data class ActivationState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    // --- Flujo de Login ---
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // --- Flujo de Activación ---
    private val _activationState = MutableStateFlow(ActivationState())
    val activationState: StateFlow<ActivationState> = _activationState.asStateFlow()

    // ==========================================
    // LÓGICA DE LOGIN MULTIPLATAFORMA
    // ==========================================
    fun login(email: String, clave: String) {
        if (email.isBlank() || clave.isBlank()) {
            _authState.value = AuthState.Error("El correo y la contraseña son obligatorios")
            return
        }
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                val uid = authRepository.login(email, clave)
                val usuario = userRepository.obtenerUsuario(uid)

                if (usuario != null) {
                    _authState.value = AuthState.Success(uid, usuario.rol)
                } else {
                    _authState.value = AuthState.Error("Usuario autenticado, pero sin perfil en la base de datos")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error al iniciar sesión")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    // ==========================================
    // LÓGICA DE PRIMER INGRESO (CAMBIO DE CONTRASEÑA)
    // ==========================================
    fun actualizarContrasenaPrimeraVez(uid: String, contrasena: String) {
        viewModelScope.launch {
            _activationState.update { it.copy(isLoading = true, error = null) }

            authRepository.cambiarContrasenaPrimeraVez(uid, contrasena)
                .onSuccess {
                    _activationState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { exception ->
                    _activationState.update { it.copy(isLoading = false, error = exception.message) }
                }
        }
    }

    fun resetActivationState() {
        _activationState.update { ActivationState() }
    }
}