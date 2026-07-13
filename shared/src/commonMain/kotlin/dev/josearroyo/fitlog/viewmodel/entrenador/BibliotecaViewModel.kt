package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.Ejercicio
import dev.josearroyo.fitlog.data.model.GrupoMuscular
import dev.josearroyo.fitlog.data.model.PlantillaRutina
import dev.josearroyo.fitlog.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BibliotecaState(
    val listaCompleta: List<Ejercicio> = emptyList(),
    val listaFiltrada: List<Ejercicio> = emptyList(),
    val grupoSeleccionado: GrupoMuscular? = null,
    val textoBusqueda: String = "",
    val isLoading: Boolean = false,
    val listaPlantillas: List<PlantillaRutina> = emptyList(),
    val isLoadingPlantillas: Boolean = false,
    val tabSeleccionado: Int = 0
)

class BibliotecaViewModel : ViewModel() {
    private val repository = ExerciseRepository()

    private val _state = MutableStateFlow(BibliotecaState())
    val state = _state.asStateFlow()

    // ==========================================
    // 🚀 CARGA DE DATOS MULTIPLATAFORMA
    // ==========================================

    fun cargarBiblioteca(entrenadorId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val ejercicios = repository.obtenerBibliotecaCompleta(entrenadorId)
                _state.update { it.copy(listaCompleta = ejercicios, isLoading = false) }
                aplicarFiltros(_state.value.textoBusqueda, _state.value.grupoSeleccionado)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun cargarPlantillas(entrenadorId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingPlantillas = true) }
            try {
                val plantillas = repository.obtenerPlantillasDelEntrenador(entrenadorId)
                _state.update { it.copy(listaPlantillas = plantillas, isLoadingPlantillas = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingPlantillas = false) }
            }
        }
    }

    // ==========================================
    // 🔍 FILTROS REACTIVOS EN MEMORIA
    // ==========================================

    fun filtrarEjercicios(texto: String, grupo: GrupoMuscular?) {
        aplicarFiltros(texto, grupo)
    }

    private fun aplicarFiltros(nuevoTexto: String, nuevoGrupo: GrupoMuscular?) {
        val resultado = _state.value.listaCompleta.filter { ejercicio ->
            val coincideNombre = ejercicio.nombre.contains(nuevoTexto, ignoreCase = true)
            val coincideGrupo = nuevoGrupo == null || ejercicio.grupoMuscular == nuevoGrupo
            coincideNombre && coincideGrupo
        }

        _state.update { it.copy(
            listaFiltrada = resultado,
            textoBusqueda = nuevoTexto,
            grupoSeleccionado = nuevoGrupo
        ) }
    }

    fun cambiarPestana(nuevoIndex: Int) {
        _state.update { it.copy(tabSeleccionado = nuevoIndex) }
    }

    // ==========================================
    // 🗑️ ELIMINACIÓN FÍSICA MULTIPLATAFORMA
    // ==========================================

    fun eliminarEjercicioPersonalizado(ejercicioId: String, entrenadorId: String) {
        viewModelScope.launch {
            try {
                repository.eliminarEjercicioFisico(ejercicioId)
                cargarBiblioteca(entrenadorId)
            } catch (e: Exception) {
                // Manejo de errores en red
            }
        }
    }

    fun eliminarPlantilla(plantillaId: String, entrenadorId: String) {
        viewModelScope.launch {
            try {
                repository.eliminarPlantillaFisica(plantillaId)
                cargarPlantillas(entrenadorId)
            } catch (e: Exception) {
                // Manejo de errores en red
            }
        }
    }
}