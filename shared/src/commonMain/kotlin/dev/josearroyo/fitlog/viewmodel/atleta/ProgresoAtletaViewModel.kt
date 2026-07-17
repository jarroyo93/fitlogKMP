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
        val ahoraMilis = getCurrentTimeMillis() //
        val unDiaMilis = 86400000L //[cite: 1]
        val unMesMilis = unDiaMilis * 30L //[cite: 1]

        // 1. Cómputo de entrenamientos del mes (Acotado de forma segura)
        val conteoMes = todasLasSesiones.count {
            it.fechaEjecucion in (ahoraMilis - unMesMilis)..ahoraMilis
        }

        // 2. Cómputo de Racha Semanal y Volumen mediante aritmética pura de tiempo
        var volumenTotal = 0.0 //[cite: 1]
        val racha = mutableListOf<Pair<String, Boolean>>() //[cite: 1]
        val nombresDiasSemana = listOf("D", "L", "M", "M", "J", "V", "S") //[cite: 1]

        for (i in 6 downTo 0) { //[cite: 1]
            val objetivoDiaTimestamp = ahoraMilis - (i * unDiaMilis) //[cite: 1]
            val inicioDia = objetivoDiaTimestamp - (objetivoDiaTimestamp % unDiaMilis) //[cite: 1]
            val finDia = inicioDia + unDiaMilis - 1 //[cite: 1]

            val sesionesDelDia = todasLasSesiones.filter { it.fechaEjecucion in inicioDia..finDia } //[cite: 1]
            val diaIndex = (((inicioDia / unDiaMilis) + 4) % 7).toInt() //[cite: 1]
            val letraDia = nombresDiasSemana[if (diaIndex < 0) diaIndex + 7 else diaIndex] //[cite: 1]

            racha.add(Pair(letraDia, sesionesDelDia.isNotEmpty())) //[cite: 1]

            sesionesDelDia.forEach { sesion -> //[cite: 1]
                sesion.ejerciciosRealizados.filter { !it.fueSaltado }.forEach { ejercicio -> //[cite: 1]
                    ejercicio.seriesRealizadas.forEach { serie -> //[cite: 1]
                        volumenTotal += (serie.pesoKg * serie.repeticionesLogradas) //[cite: 1]
                    }
                }
            }
        }

        // 3. Extraer ejercicios únicos ordenados alfabéticamente
        val nombresUnicos = todasLasSesiones
            .flatMap { it.ejerciciosRealizados } //[cite: 1]
            .map { it.nombreEjercicio } //[cite: 1]
            .distinct() //[cite: 1]
            .sorted() //[cite: 1]

        // 4. Calcular Récords Personales históricos (PRs) con desempate lógico por volumen/reps 🔥
        val recordsMap = mutableMapOf<String, RecordPersonalUI>() //[cite: 1]
        todasLasSesiones.forEach { sesion -> //[cite: 1]
            sesion.ejerciciosRealizados.filter { !it.fueSaltado }.forEach { ej -> //[cite: 1]
                ej.seriesRealizadas.forEach { serie -> //[cite: 1]
                    val recordActual = recordsMap[ej.nombreEjercicio] //[cite: 1]

                    if (recordActual == null ||
                        serie.pesoKg > recordActual.pesoMaximo ||
                        (serie.pesoKg == recordActual.pesoMaximo && serie.repeticionesLogradas > recordActual.repeticiones)
                    ) {
                        recordsMap[ej.nombreEjercicio] = RecordPersonalUI( //[cite: 1]
                            nombreEjercicio = ej.nombreEjercicio, //[cite: 1]
                            pesoMaximo = serie.pesoKg, //[cite: 1]
                            repeticiones = serie.repeticionesLogradas, //[cite: 1]
                            fechaFormateada = formatearFechaHistorial(sesion.fechaEjecucion) //[cite: 1]
                        )
                    }
                }
            }
        }

        // 5. Calcular el historial del primer ejercicio antes de emitir para evitar el parpadeo en la UI ⚡
        val historialInicialFiltrado = if (nombresUnicos.isNotEmpty()) {
            val primerEjercicio = nombresUnicos.first()
            todasLasSesiones.mapNotNull { sesion ->
                val ejercicioEncontrado = sesion.ejerciciosRealizados.find {
                    it.nombreEjercicio == primerEjercicio && !it.fueSaltado
                }
                if (ejercicioEncontrado != null) {
                    DetalleEjercicioUI(
                        fechaFormat = formatearFechaHistorial(sesion.fechaEjecucion),
                        nombreRutina = sesion.nombreRutina,
                        detalle = ejercicioEncontrado
                    )
                } else null
            }
        } else emptyList()

        // Emitimos todas las métricas calculadas en una ÚNICA actualización atómica de estado
        _uiState.update { state ->
            state.copy(
                entrenosMes = conteoMes,
                rachaSemana = racha,
                volumenSemanal = volumenTotal,
                ejerciciosDisponibles = nombresUnicos,
                recordsPersonales = recordsMap.values.sortedByDescending { it.pesoMaximo },
                historialEjercicioFiltrado = historialInicialFiltrado,
                isLoading = false
            )
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