package dev.josearroyo.fitlog.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.SesionEntrenamiento
import dev.josearroyo.fitlog.viewmodel.atleta.ProgresoAtletaViewModel
import dev.josearroyo.fitlog.viewmodel.atleta.DetalleEjercicioUI
import dev.josearroyo.fitlog.viewmodel.atleta.RecordPersonalUI
import dev.josearroyo.fitlog.formatearFechaHistorial

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgresoAtletaScreen(
    userId: String,
    onBack: (() -> Unit)? = null,
    viewModel: ProgresoAtletaViewModel = viewModel { ProgresoAtletaViewModel() }
) {
    val state by viewModel.uiState.collectAsState()
    // 🟢 CORREGIDO: Salvaguarda el estado de la pestaña ante cambios de configuración
    var tabSeleccionada by rememberSaveable { mutableStateOf(0) }
    val titulosTabs = listOf("Evolución", "Diario", "Récords")

    LaunchedEffect(userId) {
        viewModel.cargarDatosProgreso(userId)
    }

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = { Text("Rendimiento de Carga", fontWeight = FontWeight.Bold, color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = NaranjaAcento)
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(FondoOscuro), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NaranjaAcento)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FondoOscuro)
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (onBack == null) {
                    item {
                        Text(
                            text = "Mi Rendimiento",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                item {
                    KpiSection(state.rachaSemana, state.entrenosMes, state.volumenSemanal)
                }

                item {
                    TabRow(
                        selectedTabIndex = tabSeleccionada,
                        containerColor = FondoTarjeta,
                        contentColor = NaranjaAcento,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[tabSeleccionada]),
                                color = NaranjaAcento
                            )
                        }
                    ) {
                        titulosTabs.forEachIndexed { index, titulo ->
                            Tab(
                                selected = tabSeleccionada == index,
                                onClick = { tabSeleccionada = index },
                                text = { Text(titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                                selectedContentColor = NaranjaAcento,
                                unselectedContentColor = TextoSecundario
                            )
                        }
                    }
                }

                when (tabSeleccionada) {
                    0 -> {
                        item {
                            EvolucionEjercicioSection(
                                ejerciciosDisponibles = state.ejerciciosDisponibles,
                                historialEjercicio = state.historialEjercicioFiltrado,
                                onEjercicioSeleccionado = { viewModel.filtrarPorEjercicio(it) }
                            )
                        }
                    }
                    1 -> {
                        if (state.historialSesiones.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No hay bitácoras de entrenamiento registradas.", color = TextoSecundario)
                                }
                            }
                        } else {
                            // 🟢 CORREGIDO: Clave única inmutable para el reciclaje del LazyColumn
                            items(state.historialSesiones, key = { it.fechaEjecucion }) { sesion ->
                                TarjetaDiarioSesion(sesion = sesion)
                            }
                        }
                    }
                    2 -> {
                        if (state.recordsPersonales.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("Ningún récord personal guardado aún.", color = TextoSecundario)
                                }
                            }
                        } else {
                            // 🟢 CORREGIDO: Clave de identidad estable para evitar recomposiciones
                            items(state.recordsPersonales, key = { it.nombreEjercicio }) { record ->
                                TarjetaRecordPersonal(record = record)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaDiarioSesion(sesion: SesionEntrenamiento) {
    // 🟢 CORREGIDO: El diálogo no desaparece si ocurre un rediseño de la UI
    var mostrarDetalleDialog by rememberSaveable { mutableStateOf(false) }

    val volumenSesion = remember(sesion.ejerciciosRealizados) {
        sesion.ejerciciosRealizados.filter { !it.fueSaltado }.sumOf { ej ->
            ej.seriesRealizadas.sumOf { serie -> serie.pesoKg * serie.repeticionesLogradas }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { mostrarDetalleDialog = true },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = sesion.nombreRutina, fontWeight = FontWeight.Bold, color = NaranjaAcento, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = formatearFechaHistorial(sesion.fechaEjecucion), color = TextoSecundario, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = FondoOscuro)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "${sesion.ejerciciosRealizados.size} Ejercicios ejecutados", color = Color.White, fontSize = 14.sp)
                Text(text = "Volumen: ${volumenSesion.toInt()} kg", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
        }
    }

    if (mostrarDetalleDialog) {
        DetalleSesionDialog(sesion = sesion, onDismiss = { mostrarDetalleDialog = false })
    }
}

@Composable
fun DetalleSesionDialog(sesion: SesionEntrenamiento, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(sesion.nombreRutina, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                Text(text = "Fecha: ${formatearFechaHistorial(sesion.fechaEjecucion)}", color = TextoSecundario, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = FondoOscuro)

                LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(sesion.ejerciciosRealizados, key = { it.nombreEjercicio }) { ej ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "${ej.ordenSecuencia + 1}. ${ej.nombreEjercicio}", fontWeight = FontWeight.Bold, color = NaranjaAcento, modifier = Modifier.weight(1f), fontSize = 16.sp)
                                if (ej.fueSaltado) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("Saltado", fontWeight = FontWeight.Bold, color = Color(0xFFE57373)) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFE57373).copy(alpha = 0.1f))
                                    )
                                }
                            }

                            if (ej.fueSaltado) {
                                Text(text = "Justificación: ${ej.justificacionSalto.ifBlank { "No especificado" }}", fontStyle = FontStyle.Italic, color = Color(0xFFE57373), modifier = Modifier.padding(start = 16.dp, top = 4.dp), fontSize = 14.sp)
                            } else {
                                ej.seriesRealizadas.forEach { serie ->
                                    val repMeta = serie.repsTarget
                                    val repLogradas = serie.repeticionesLogradas

                                    val (colorResaltado, subTextoComparativa) = remember(repLogradas, repMeta) {
                                        when {
                                            repLogradas < repMeta -> Color(0xFFE57373) to "Pauta: $repMeta (Faltaron ${repMeta - repLogradas})"
                                            repLogradas > repMeta -> Color(0xFF81C784) to "Pauta: $repMeta (+${repLogradas - repMeta} ¡Superado!)"
                                            else -> Color(0xFF4FC3F7) to "Pauta: $repMeta (Completado)"
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("S${serie.numeroSerie}: ", color = TextoSecundario, fontSize = 14.sp)
                                                Text("${serie.pesoKg} kg x $repLogradas reps", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                            }
                                            Text(text = subTextoComparativa, color = colorResaltado, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(FondoOscuro).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                            Text(text = "RPE: ${serie.rpe?.toString() ?: "-"}", fontWeight = FontWeight.Black, color = NaranjaAcento, fontSize = 12.sp)
                                        }
                                    }
                                }

                                if (ej.notasAtleta.isNotBlank()) {
                                    Row(modifier = Modifier.padding(start = 16.dp, top = 4.dp), verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = TextoSecundario, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Nota: ${ej.notasAtleta}", fontStyle = FontStyle.Italic, color = TextoSecundario, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = FondoOscuro)
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cerrar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun KpiSection(rachaSemana: List<Pair<String, Boolean>>, entrenosMes: Int, volumenSemanal: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Consistencia (Últimos 7 días)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    rachaSemana.forEach { (dia, entrenado) ->
                        DiaRachaItem(dia = dia, entrenado = entrenado)
                    }
                }

                HorizontalDivider(color = FondoOscuro)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    MiniKpi(valor = "$entrenosMes", etiqueta = "Sesiones del mes")
                    MiniKpi(valor = "${volumenSemanal.toInt()} kg", etiqueta = "Carga Semanal total")
                }
            }
        }
    }
}

@Composable
fun DiaRachaItem(dia: String, entrenado: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(if (entrenado) NaranjaAcento else FondoOscuro),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dia,
                color = if (entrenado) FondoOscuro else TextoSecundario,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun MiniKpi(valor: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = valor, fontWeight = FontWeight.Black, color = Color.White, fontSize = 20.sp)
        Text(text = etiqueta, color = TextoSecundario, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvolucionEjercicioSection(
    ejerciciosDisponibles: List<String>,
    historialEjercicio: List<DetalleEjercicioUI>,
    onEjercicioSeleccionado: (String) -> Unit
) {
    var expandirMenu by rememberSaveable { mutableStateOf(false) }
    var ejercicioActual by remember(ejerciciosDisponibles) {
        mutableStateOf(ejerciciosDisponibles.firstOrNull() ?: "Seleccionar Ejercicio")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Historial por Ejercicio", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

        if (ejerciciosDisponibles.isEmpty()) {
            Text("No se registran patrones de fuerza aún.", color = TextoSecundario, fontSize = 14.sp)
            return
        }

        Card(
            onClick = { expandirMenu = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = ejercicioActual, fontWeight = FontWeight.Bold, color = Color.White)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Seleccionar", tint = NaranjaAcento)
            }

            DropdownMenu(expanded = expandirMenu, onDismissRequest = { expandirMenu = false }, modifier = Modifier.background(FondoTarjeta)) {
                ejerciciosDisponibles.forEach { ejercicio ->
                    DropdownMenuItem(
                        text = { Text(ejercicio, color = Color.White, fontWeight = FontWeight.Medium) },
                        onClick = {
                            ejercicioActual = ejercicio
                            expandirMenu = false
                            onEjercicioSeleccionado(ejercicio)
                        }
                    )
                }
            }
        }

        if (historialEjercicio.isNotEmpty()) {
            GraficaProgresoEjercicio(historialEjercicio = historialEjercicio)
        }

        historialEjercicio.forEach { registro ->
            key(registro.fechaFormat) {
                RegistroEjercicioCard(registro)
            }
        }
    }
}

@Composable
fun RegistroEjercicioCard(registro: DetalleEjercicioUI) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(registro.fechaFormat, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text(registro.nombreRutina, color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            registro.detalle.seriesRealizadas.forEach { serie ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Serie ${serie.numeroSerie}", color = TextoSecundario, fontSize = 14.sp)
                    Text("${serie.pesoKg} kg x ${serie.repeticionesLogradas} reps", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun TarjetaRecordPersonal(record: RecordPersonalUI) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NaranjaAcento.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaranjaAcento.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = "Récord", tint = Color(0xFFFFD700), modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.nombreEjercicio,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(text = "Marcado el: ${record.fechaFormateada}", color = TextoSecundario, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${if(record.pesoMaximo % 1.0 == 0.0) record.pesoMaximo.toInt() else record.pesoMaximo} kg", fontWeight = FontWeight.Black, color = NaranjaAcento, fontSize = 20.sp)
                Text(text = "x ${record.repeticiones} reps", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun GraficaProgresoEjercicio(historialEjercicio: List<DetalleEjercicioUI>) {
    val ultimosRegistros = remember(historialEjercicio) { historialEjercicio.take(6).reversed() }

    val datosVolumen = remember(ultimosRegistros) {
        ultimosRegistros.map { reg ->
            reg.detalle.seriesRealizadas.sumOf { s -> s.pesoKg * s.repeticionesLogradas }.toFloat()
        }
    }

    val etiquetasFechas = remember(ultimosRegistros) {
        ultimosRegistros.map { reg ->
            val partes = reg.fechaFormat.split(" ")
            if (partes.size >= 2) "${partes[0]} ${partes[1]}" else reg.fechaFormat
        }
    }

    if (datosVolumen.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Curva de Rendimiento", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Text("Volumen por sesión (kg × reps)", color = TextoSecundario, fontSize = 12.sp)
                }
                Text(text = "${datosVolumen.lastOrNull()?.toInt() ?: 0} kg", fontWeight = FontWeight.Black, color = NaranjaAcento, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            val maxVal = datosVolumen.maxOrNull() ?: 1f
            val minVal = datosVolumen.minOrNull() ?: 0f
            val rango = if (maxVal == minVal) 1f else maxVal - minVal

            // 🟢 CORREGIDO: Lógica geométrica calculada fuera de la ejecución del pincel de dibujo de Canvas
            Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                val ancho = size.width
                val alto = size.height
                val espacioX = ancho / (if (datosVolumen.size > 1) datosVolumen.size - 1 else 1)

                val puntos = datosVolumen.mapIndexed { i, valor ->
                    val x = i * espacioX
                    val y = alto - ((valor - minVal) / rango) * (alto * 0.8f) - (alto * 0.1f)
                    androidx.compose.ui.geometry.Offset(x, y)
                }

                if (puntos.size > 1) {
                    // El Path se gestiona dinámicamente sin generar acumulación de Garbage Collection
                    val pathFondo = Path().apply {
                        moveTo(puntos.first().x, alto)
                        puntos.forEach { lineTo(it.x, it.y) }
                        lineTo(puntos.last().x, alto)
                        close()
                    }
                    drawPath(path = pathFondo, brush = Brush.verticalGradient(colors = listOf(NaranjaAcento.copy(alpha = 0.25f), Color.Transparent)))

                    val pathLinea = Path().apply {
                        moveTo(puntos.first().x, puntos.first().y)
                        for (i in 1 until puntos.size) { lineTo(puntos[i].x, puntos[i].y) }
                    }
                    drawPath(path = pathLinea, color = NaranjaAcento, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }

                puntos.forEach { punto ->
                    drawCircle(color = FondoOscuro, radius = 5.dp.toPx(), center = punto)
                    drawCircle(color = NaranjaAcento, radius = 3.5.dp.toPx(), center = punto)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                etiquetasFechas.forEach { fecha ->
                    Text(text = fecha, color = TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}