package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.repository.UserRepository
import dev.josearroyo.fitlog.repository.AtletaProgresoRepository
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.esMismoDia
import dev.josearroyo.fitlog.formatearHora
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AsistenciaAtletaUI(
    val atleta: Usuario,
    val asistio: Boolean,
    val horaEntrenamiento: String? = null
)

class EntrenadorViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val progresoRepository = AtletaProgresoRepository()

    private val _atletas = MutableStateFlow<List<Usuario>>(emptyList())
    val atletas: StateFlow<List<Usuario>> = _atletas.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _codigoGenerado = MutableStateFlow<String?>(null)
    val codigoGenerado: StateFlow<String?> = _codigoGenerado.asStateFlow()

    private val _isGeneratingCode = MutableStateFlow(false)
    val isGeneratingCode: StateFlow<Boolean> = _isGeneratingCode.asStateFlow()

    private val _asistenciaDia = MutableStateFlow<List<AsistenciaAtletaUI>>(emptyList())
    val asistenciaDia: StateFlow<List<AsistenciaAtletaUI>> = _asistenciaDia.asStateFlow()

    private val _isLoadingAsistencia = MutableStateFlow(false)
    val isLoadingAsistencia: StateFlow<Boolean> = _isLoadingAsistencia.asStateFlow()

    fun cargarAtletas(entrenadorId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val lista = userRepository.obtenerAtletasPorEntrenador(entrenadorId)
            _atletas.value = lista
            _isLoading.value = false
        }
    }

    // 🔥 Modificado: Recibe y evalúa marcas de tiempo puras tipo Long compatibles a nivel multiplataforma
    fun cargarAsistenciaPorDia(entrenadorId: String, fechaTarget: Long = getCurrentTimeMillis()) {
        viewModelScope.launch {
            _isLoadingAsistencia.value = true
            try {
                val listaAtletas = userRepository.obtenerAtletasPorEntrenador(entrenadorId)

                val reporteFinal = listaAtletas.map { atleta ->
                    val historial = progresoRepository.obtenerHistorialEntrenamientos(atleta.id)

                    // Evaluamos la asistencia usando la función puente esMismoDia
                    val sesionDeHoy = historial.find { sesion ->
                        esMismoDia(sesion.fechaEjecucion, fechaTarget)
                    }

                    // Formateamos la hora del entrenamiento usando formatearHora
                    val horaFormateada = sesionDeHoy?.let {
                        formatearHora(it.fechaEjecucion)
                    }

                    AsistenciaAtletaUI(
                        atleta = atleta,
                        asistio = sesionDeHoy != null,
                        horaEntrenamiento = horaFormateada
                    )
                }
                _asistenciaDia.value = reporteFinal
            } catch (e: Exception) {
                // Failsafe multiplataforma silencioso
            } finally {
                _isLoadingAsistencia.value = false
            }
        }
    }

    fun generarCodigoVinculacion(entrenadorId: String) {
        viewModelScope.launch {
            _isGeneratingCode.value = true
            val codigo = userRepository.generarCodigoVinculacion(entrenadorId)
            _codigoGenerado.value = codigo
            _isGeneratingCode.value = false
        }
    }

    fun limpiarCodigo() {
        _codigoGenerado.value = null
    }
}