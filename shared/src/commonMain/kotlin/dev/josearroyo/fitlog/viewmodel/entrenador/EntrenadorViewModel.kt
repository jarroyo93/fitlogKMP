package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.data.model.SesionEntrenamiento
import dev.josearroyo.fitlog.repository.UserRepository
import dev.josearroyo.fitlog.repository.AtletaProgresoRepository
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.esMismoDia
import dev.josearroyo.fitlog.formatearHora
import dev.josearroyo.fitlog.formatearFechaHora
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

    // Cache interno para aplicar búsquedas en memoria sin volver a golpear a Firestore
    private var listaAtletasCompleta: List<Usuario> = emptyList()
    private var listaAsistenciaCompleta: List<AsistenciaAtletaUI> = emptyList()

    // --- BLOQUE DE FLUJOS COMPATIBLES CON TU PANTALLA ---
    private val _atletas = MutableStateFlow<List<Usuario>>(emptyList())
    val atletas: StateFlow<List<Usuario>> = _atletas.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _codigoGenerado = MutableStateFlow<String?>(null)
    val codigoGenerado: StateFlow<String?> = _codigoGenerado.asStateFlow()

    private val _expiracionCodigoTexto = MutableStateFlow<String?>(null)
    val expiracionCodigoTexto: StateFlow<String?> = _expiracionCodigoTexto.asStateFlow()

    private val _isGeneratingCode = MutableStateFlow(false)
    val isGeneratingCode: StateFlow<Boolean> = _isGeneratingCode.asStateFlow()

    private val _asistenciaDia = MutableStateFlow<List<AsistenciaAtletaUI>>(emptyList())
    val asistenciaDia: StateFlow<List<AsistenciaAtletaUI>> = _asistenciaDia.asStateFlow()

    private val _isLoadingAsistencia = MutableStateFlow(false)
    val isLoadingAsistencia: StateFlow<Boolean> = _isLoadingAsistencia.asStateFlow()

    private val _textoBusqueda = MutableStateFlow("")
    val textoBusqueda: StateFlow<String> = _textoBusqueda.asStateFlow()

    private val _tabSeleccionado = MutableStateFlow(0) // 0 = Atletas, 1 = Asistencia
    val tabSeleccionado: StateFlow<Int> = _tabSeleccionado.asStateFlow()


    fun cargarDashboard(entrenadorId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Consumo exacto de tu UserRepository para traer alumnos vinculados
                listaAtletasCompleta = userRepository.obtenerAtletasPorEntrenador(entrenadorId)

                // 2. Consumo exacto de tu UserRepository para validar si el coach ya tiene código activo
                val perfilCoach = userRepository.obtenerUsuario(entrenadorId)
                val ahora = getCurrentTimeMillis()

                if (perfilCoach?.codigoVinculacion != null && (perfilCoach.expiracionCodigo ?: 0L) > ahora) {
                    _codigoGenerado.value = perfilCoach.codigoVinculacion
                    _expiracionCodigoTexto.value = formatearFechaHora(perfilCoach.expiracionCodigo!!)
                } else {
                    _codigoGenerado.value = null
                    _expiracionCodigoTexto.value = null
                }

                // Ejecutamos la ordenación del filtro inicial por si quedó algún remanente en la barra de búsqueda
                aplicarBusqueda(_textoBusqueda.value)

                // 3. Pasamos a procesar la asistencia del día actual
                cargarAsistenciaDelDia()

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun cargarAsistenciaDelDia() {
        viewModelScope.launch {
            _isLoadingAsistencia.value = true
            try {
                val hoy = getCurrentTimeMillis()

                // Mapeamos de forma asíncrona comparando contra el AtletaProgresoRepository real
                listaAsistenciaCompleta = listaAtletasCompleta.map { atleta ->
                    val historial = progresoRepository.obtenerHistorialEntrenamientos(atleta.id)

                    // Evaluamos usando la función expect/actual esMismoDia de tu Platform.kt
                    val sesionDeHoy = historial.find { esMismoDia(it.fechaEjecucion, hoy) }

                    AsistenciaAtletaUI(
                        atleta = atleta,
                        asistio = sesionDeHoy != null,
                        horaEntrenamiento = sesionDeHoy?.let { formatearHora(it.fechaEjecucion) }
                    )
                }

                aplicarBusqueda(_textoBusqueda.value)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingAsistencia.value = false
            }
        }
    }

    fun aplicarBusqueda(query: String) {
        _textoBusqueda.value = query

        // Filtramos la pestaña de Atletas
        _atletas.value = listaAtletasCompleta.filter {
            "${it.nombres} ${it.apellidos}".contains(query, ignoreCase = true)
        }

        // Filtramos la pestaña de Asistencia
        _asistenciaDia.value = listaAsistenciaCompleta.filter {
            "${it.atleta.nombres} ${it.atleta.apellidos}".contains(query, ignoreCase = true)
        }
    }

    fun cambiarTab(index: Int) {
        _tabSeleccionado.value = index
    }

    fun generarCodigoVinculacion(entrenadorId: String) {
        viewModelScope.launch {
            _isGeneratingCode.value = true
            try {
                // Invocamos tu método real de UserRepository que modifica la DB y genera los 6 dígitos
                val codigo = userRepository.generarCodigoVinculacion(entrenadorId)
                _codigoGenerado.value = codigo

                // Tu repositorio le suma 900,000 milisegundos (15 minutos) a la expiración.
                // Reflejamos ese cálculo aquí para pintar el texto en la UI de inmediato.
                val tiempoExpiracion = getCurrentTimeMillis() + 900000
                _expiracionCodigoTexto.value = formatearFechaHora(tiempoExpiracion)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isGeneratingCode.value = false
            }
        }
    }

    fun limpiarCodigo() {
        _codigoGenerado.value = null
        _expiracionCodigoTexto.value = null
    }
}