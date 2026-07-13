package dev.josearroyo.fitlog.viewmodel.atleta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.Habitos
import dev.josearroyo.fitlog.repository.AtletaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ==========================================
// ESTADO DEL FORMULARIO DE HÁBITOS
// ==========================================
data class AddHabitosState(
    val habitos: Habitos = Habitos(),
    val isGuardado: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AddHabitosViewModel : ViewModel() {
    private val repository = AtletaRepository()
    private val _state = MutableStateFlow(AddHabitosState())
    val state = _state.asStateFlow()

    fun actualizarHabitos(nuevo: Habitos) {
        _state.update { it.copy(habitos = nuevo) }
    }

    fun guardar(atletaId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val exito = repository.guardarHabitos(atletaId, _state.value.habitos)
            if (exito) {
                _state.update { it.copy(isLoading = false, isGuardado = true) }
            } else {
                _state.update { it.copy(isLoading = false, error = "Error al conectar con la base de datos") }
            }
        }
    }
}

// ==========================================
// ESTADO E HISTORIAL DE HÁBITOS
// ==========================================
data class HistorialHabitosState(
    val lista: List<Habitos> = emptyList(),
    val isLoading: Boolean = false
)

class HistorialHabitosViewModel : ViewModel() {
    private val repository = AtletaRepository()
    private val _state = MutableStateFlow(HistorialHabitosState())
    val state = _state.asStateFlow()

    fun cargarHistorial(atletaId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val lista = repository.obtenerHistorialHabitos(atletaId)
            _state.update { it.copy(lista = lista, isLoading = false) }
        }
    }
}