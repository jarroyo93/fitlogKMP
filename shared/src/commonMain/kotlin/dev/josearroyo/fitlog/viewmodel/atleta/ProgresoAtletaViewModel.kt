package dev.josearroyo.fitlog.viewmodel.atleta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.SesionEntrenamiento
import dev.josearroyo.fitlog.data.model.EjercicioRealizado
import dev.josearroyo.fitlog.repository.AtletaProgresoRepository
import dev.josearroyo.fitlog.repository.UserRepository
import dev.josearroyo.fitlog.getCurrentTimeMillis // ⚡ Helper nativo milisegundos Unix
import dev.josearroyo.fitlog.formatearFechaHistorial
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Modelos UI para mapeo limpio en commonMain
data class DetalleEjercicioUI(
    val fechaFormat: String,
    val nombreRutina: String,
    val detalle: EjercicioRealizado
)

data class RecordPersonalUI(
    val nombreEjercicio: String,
    val pesoMaximo: Double,
    val repeticiones: Int,
    val fechaFormateada: String
)

// Estado unificado y seguro para congelamiento de memoria en iOS
data class ProgresoAtletaState(
    val isLoading: Boolean = true,
    val rachaSemana: List<Pair<String, Boolean>> = emptyList(),
    val entrenosMes: Int = 0,
    val volumenSemanal: Double = 0.0,
    val ejerciciosDisponibles: List<String> = emptyList(),
    val historialEjercicioFiltrado: List<DetalleEjercicioUI> = emptyList(),
    val historialSesiones: List<SesionEntrenamiento> = emptyList(),
    val recordsPersonales: List<RecordPersonalUI> = emptyList()
)

class ProgresoAtletaViewModel(
    private val repository: AtletaProgresoRepository = AtletaProgresoRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgresoAtletaState())
    val uiState: StateFlow<ProgresoAtletaState> = _uiState.asStateFlow()

    private var todasLasSesiones: List<SesionEntrenamiento> = emptyList()

    fun cargarDatosProgreso(authUid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val usuario = userRepository.obtenerUsuario(authUid)
                if (usuario != null) {
                    todasLasSesiones = repository.obtenerHistorialEntrenamientos(usuario.id)

                    _uiState.update { state ->
                        state.copy(
                            historialSesiones = todasLasSesiones,
                            isLoading = false
                        )
                    }
                    calcularKPIsYEjercicios()
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun calcularKPIsYEjercicios() {
        val ahoraMilis = getCurrentTimeMillis()
        val unDiaMilis = 86400000L
        val unMesMilis = unDiaMilis * 30L // Aproximación segura para KPI de consistencia del mes comercial

        // 1. Cómputo de entrenamientos del mes
        val conteoMes = todasLasSesiones.count {
            (ahoraMilis - it.fechaEjecucion) <= unMesMilis
        }

        // 2. Cómputo de Racha Semanal y Volumen mediante aritmética pura de tiempo
        var volumenTotal = 0.0
        val racha = mutableListOf<Pair<String, Boolean>>()

        // El Epoch de 1970 inició un Jueves. Calculamos el día de la semana actual con aritmética modular
        val nombresDiasSemana = listOf("D", "L", "M", "M", "J", "V", "S") // Dom, Lun, Mar, Mié, Jue, Vie, Sáb

        for (i in 6 downTo 0) {
            val objetivoDiaTimestamp = ahoraMilis - (i * unDiaMilis)

            // Delimitamos el inicio y fin absoluto de ese día (rango de 24 horas)
            val inicioDia = objetivoDiaTimestamp - (objetivoDiaTimestamp % unDiaMilis)
            val finDia = inicioDia + unDiaMilis - 1

            val sesionesDelDia = todasLasSesiones.filter { it.fechaEjecucion in inicioDia..finDia }

            // Resolvemos el caracter del día de la semana correspondiente a ese timestamp
            val diaIndex = (((inicioDia / unDiaMilis) + 4) % 7).toInt() // +4 desplaza al Jueves base de Epoch
            val letraDia = nombresDiasSemana[if (diaIndex < 0) diaIndex + 7 else diaIndex]

            racha.add(Pair(letraDia, sesionesDelDia.isNotEmpty()))

            // Sumamos el volumen total de carga ejecutada en la semana activa
            sesionesDelDia.forEach { sesion ->
                sesion.ejerciciosRealizados.filter { !it.fueSaltado }.forEach { ejercicio ->
                    ejercicio.seriesRealizadas.forEach { serie ->
                        volumenTotal += (serie.pesoKg * serie.repeticionesLogradas)
                    }
                }
            }
        }

        // 3. Extraer ejercicios únicos ordenados alfabéticamente
        val nombresUnicos = todasLasSesiones
            .flatMap { it.ejerciciosRealizados }
            .map { it.nombreEjercicio }
            .distinct()
            .sorted()

        // 4. Calcular Récords Personales históricos (PRs)
        val recordsMap = mutableMapOf<String, RecordPersonalUI>()
        todasLasSesiones.forEach { sesion ->
            sesion.ejerciciosRealizados.filter { !it.fueSaltado }.forEach { ej ->
                ej.seriesRealizadas.forEach { serie ->
                    val recordActual = recordsMap[ej.nombreEjercicio]
                    if (recordActual == null || serie.pesoKg > recordActual.pesoMaximo) {
                        recordsMap[ej.nombreEjercicio] = RecordPersonalUI(
                            nombreEjercicio = ej.nombreEjercicio,
                            pesoMaximo = serie.pesoKg,
                            repeticiones = serie.repeticionesLogradas,
                            fechaFormateada = formatearFechaHistorial(sesion.fechaEjecucion)
                        )
                    }
                }
            }
        }

        // Emitimos todas las métricas calculadas en un solo ciclo de actualización atómica
        _uiState.update { state ->
            state.copy(
                entrenosMes = conteoMes,
                rachaSemana = racha,
                volumenSemanal = volumenTotal,
                ejerciciosDisponibles = nombresUnicos,
                recordsPersonales = recordsMap.values.sortedByDescending { it.pesoMaximo }
            )
        }

        if (nombresUnicos.isNotEmpty()) {
            filtrarPorEjercicio(nombresUnicos.first())
        }
    }

    fun filtrarPorEjercicio(nombreEjercicio: String) {
        val historialFiltrado = todasLasSesiones.mapNotNull { sesion ->
            val ejercicioEncontrado = sesion.ejerciciosRealizados.find {
                it.nombreEjercicio == nombreEjercicio && !it.fueSaltado
            }

            if (ejercicioEncontrado != null) {
                DetalleEjercicioUI(
                    fechaFormat = formatearFechaHistorial(sesion.fechaEjecucion),
                    nombreRutina = sesion.nombreRutina,
                    detalle = ejercicioEncontrado
                )
            } else null
        }

        _uiState.update { it.copy(historialEjercicioFiltrado = historialFiltrado) }
    }
}