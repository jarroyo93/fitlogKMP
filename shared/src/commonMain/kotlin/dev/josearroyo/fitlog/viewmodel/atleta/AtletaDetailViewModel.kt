package dev.josearroyo.fitlog.viewmodel.atleta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.data.model.CicloEntrenamiento
import dev.josearroyo.fitlog.repository.UserRepository
import dev.josearroyo.fitlog.repository.AtletaProgresoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AtletaDetailState(
    val atleta: Usuario? = null,
    val cicloActivo: CicloEntrenamiento? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class AtletaDetailViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val progresoRepository = AtletaProgresoRepository()

    private val _state = MutableStateFlow(AtletaDetailState())
    val state = _state.asStateFlow()

    fun cargarExpedienteAtleta(atletaId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // 1. Cargamos el perfil completo del Atleta
                val usuario = userRepository.obtenerUsuario(atletaId)

                if (usuario != null) {
                    // 2. Revisamos si tiene un plan de entrenamiento activo corriendo
                    val ciclo = progresoRepository.obtenerCicloActivo(atletaId)

                    _state.update { it.copy(
                        atleta = usuario,
                        cicloActivo = ciclo,
                        isLoading = false
                    ) }
                } else {
                    _state.update { it.copy(isLoading = false, error = "El atleta no existe en el sistema.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error al conectar con Firestore") }
            }
        }
    }
}