package dev.josearroyo.fitlog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.calcularFechaFinSuscripcion
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.data.model.EstadoSuscripcion
import dev.josearroyo.fitlog.data.model.TipoPlanSuscripcion
import dev.josearroyo.fitlog.data.model.EstadoPeriodo // 🚀 Importación obligatoria
import dev.josearroyo.fitlog.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days

enum class FiltroFacturacion(val etiqueta: String) {
    TODOS("Todos"),
    ACTIVOS("Activos"),
    PROXIMOS_A_VENCER("Próximos (3 días)"),
    PAUSADOS("Pausados"),
    VENCIDOS("Vencidos"),
    SIN_PLAN("Sin Plan")
}

data class FacturacionState(
    val isLoading: Boolean = true,
    val atletas: List<Usuario> = emptyList(),
    val atletasFiltrados: List<Usuario> = emptyList(),
    val searchQuery: String = "",
    val filtroActual: FiltroFacturacion = FiltroFacturacion.TODOS
)

class FacturacionViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val _state = MutableStateFlow(FacturacionState())
    val state = _state.asStateFlow()

    fun cargarAtletas(entrenadorId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val lista = userRepository.obtenerAtletasPorEntrenador(entrenadorId)
                _state.update { it.copy(atletas = lista, isLoading = false) }
                aplicarFiltros()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
        aplicarFiltros()
    }

    fun onFiltroChanged(filtro: FiltroFacturacion) {
        _state.update { it.copy(filtroActual = filtro) }
        aplicarFiltros()
    }

    private fun aplicarFiltros() {
        val query = _state.value.searchQuery.lowercase()
        val filtro = _state.value.filtroActual
        val ahora = getCurrentTimeMillis()
        val tresDiasEnMillis = 3.days.inWholeMilliseconds

        val filtrados = _state.value.atletas.filter { atleta ->
            val coincideBusqueda = atleta.nombres.lowercase().contains(query) ||
                    atleta.apellidos.lowercase().contains(query) ||
                    atleta.correo.lowercase().contains(query)

            val coincideFiltro = when (filtro) {
                FiltroFacturacion.TODOS -> true
                FiltroFacturacion.ACTIVOS -> atleta.estadoSuscripcion == EstadoSuscripcion.ACTIVO
                FiltroFacturacion.PROXIMOS_A_VENCER -> {
                    val vencimiento = atleta.vencimientoSuscripcion ?: 0L
                    atleta.estadoSuscripcion == EstadoSuscripcion.ACTIVO &&
                            (vencimiento - ahora) in 0..tresDiasEnMillis
                }
                FiltroFacturacion.PAUSADOS -> atleta.estadoSuscripcion == EstadoSuscripcion.SUSPENDIDO && atleta.saldoMilisegundosRestantes != null
                FiltroFacturacion.VENCIDOS -> atleta.estadoSuscripcion == EstadoSuscripcion.VENCIDO
                FiltroFacturacion.SIN_PLAN -> atleta.estadoSuscripcion == EstadoSuscripcion.HUERFANO || atleta.planActivo == "Ninguno"
            }

            coincideBusqueda && coincideFiltro
        }
        _state.update { it.copy(atletasFiltrados = filtrados) }
    }

    fun renovarAtleta(
        atletaId: String,
        entrenadorId: String,
        tipoPlan: TipoPlanSuscripcion,
        diasPersonalizados: Int,
        iniciarEnseguida: Boolean,
        fechaInicioSeleccionada: Long
    ) {
        viewModelScope.launch {
            val atleta = _state.value.atletas.find { it.id == atletaId } ?: return@launch
            val ahora = getCurrentTimeMillis()

            val fechaInicioLong = if (iniciarEnseguida) {
                val vencimientoBase = atleta.vencimientoSuscripcion ?: 0L
                if (vencimientoBase > ahora) vencimientoBase + 1000L else ahora
            } else {
                fechaInicioSeleccionada
            }

            val diasDelPlan = if (tipoPlan == TipoPlanSuscripcion.PERSONALIZADO) {
                diasPersonalizados
            } else {
                tipoPlan.dias
            }

            val fechaFinLong = calcularFechaFinSuscripcion(fechaInicioLong, diasDelPlan)

            // 🚀 DETERMINAR SI EL NUEVO PERIODO INICIA EN DIFERIDO O ACTIVO
            val tienePlanActivoCorriendo = atleta.estadoSuscripcion == EstadoSuscripcion.ACTIVO &&
                    (atleta.vencimientoSuscripcion ?: 0L) > ahora
            val estadoPeriodoCalculado = if (tienePlanActivoCorriendo) EstadoPeriodo.DIFERIDO else EstadoPeriodo.ACTIVO

            _state.update { it.copy(isLoading = true) }
            val exito = userRepository.renovarSuscripcion(
                atletaId = atletaId,
                entrenadorId = entrenadorId,
                planActivo = tipoPlan.name,           // 🚀 Corregido: Mapeado a String
                fechaInicio = fechaInicioLong,
                fechaFin = fechaFinLong,
                estadoPeriodo = estadoPeriodoCalculado // 🚀 Corregido: Envío del Enum esperado
            )
            if (exito) {
                cargarAtletas(entrenadorId)
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun pausarAtleta(atletaId: String, entrenadorId: String, motivo: String) {
        viewModelScope.launch {
            val atleta = _state.value.atletas.find { it.id == atletaId } ?: return@launch
            val vencimiento = atleta.vencimientoSuscripcion ?: 0L
            val ahora = getCurrentTimeMillis()

            val saldoMilis = if (vencimiento > ahora) vencimiento - ahora else 0L

            _state.update { it.copy(isLoading = true) }
            val exito = userRepository.pausarAtleta(atletaId, motivo, saldoMilis)
            if (exito) {
                cargarAtletas(entrenadorId)
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun reactivarAtleta(atletaId: String, entrenadorId: String) {
        viewModelScope.launch {
            val atleta = _state.value.atletas.find { it.id == atletaId } ?: return@launch
            val saldoMilis = atleta.saldoMilisegundosRestantes ?: 0L
            val ahora = getCurrentTimeMillis()

            val nuevaFechaFin = ahora + saldoMilis

            _state.update { it.copy(isLoading = true) }
            // 🚀 Corregido: Eliminado entrenadorId para satisfacer el formato del Repository (2 parámetros)
            val exito = userRepository.reactivarAtleta(atletaId, nuevaFechaFin)
            if (exito) {
                cargarAtletas(entrenadorId)
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}