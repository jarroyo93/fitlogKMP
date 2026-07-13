package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.Ejercicio
import dev.josearroyo.fitlog.data.model.GrupoMuscular
import dev.josearroyo.fitlog.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddEjercicioViewModel : ViewModel() {
    private val repository = ExerciseRepository()

    private val _isSaved = MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()

    private val _nombre = MutableStateFlow("")
    val nombre = _nombre.asStateFlow()

    private val _grupoMuscular = MutableStateFlow(GrupoMuscular.PECHO)
    val grupoMuscular = _grupoMuscular.asStateFlow()

    private var ejercicioIdActual: String? = null

    fun actualizarNombre(nuevoNombre: String) { _nombre.value = nuevoNombre }
    fun actualizarGrupo(nuevoGrupo: GrupoMuscular) { _grupoMuscular.value = nuevoGrupo }

    fun cargarEjercicioSiExiste(ejercicioId: String?) {
        if (ejercicioId == null || ejercicioId == "null") return

        ejercicioIdActual = ejercicioId
        viewModelScope.launch {
            val ejercicioExistente = repository.obtenerEjercicioPorId(ejercicioId)
            if (ejercicioExistente != null) {
                _nombre.value = ejercicioExistente.nombre
                _grupoMuscular.value = ejercicioExistente.grupoMuscular
            }
        }
    }

    fun guardarEjercicio(entrenadorId: String) {
        viewModelScope.launch {
            try {
                if (ejercicioIdActual != null) {
                    val datosActualizados = mapOf(
                        "nombre" to _nombre.value,
                        "grupoMuscular" to _grupoMuscular.value.name
                    )
                    repository.actualizarEjercicioPersonalizado(ejercicioIdActual!!, datosActualizados)
                } else {
                    val nuevoEjercicio = Ejercicio(
                        nombre = _nombre.value,
                        grupoMuscular = _grupoMuscular.value,
                        esPersonalizado = true,
                        creadorId = entrenadorId,
                        activo = true
                    )
                    repository.guardarEjercicioPersonalizado(nuevoEjercicio)
                }
                _isSaved.value = true
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}