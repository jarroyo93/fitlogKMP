package dev.josearroyo.fitlog.ui.entrenador

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.data.model.EstadoSuscripcion
import dev.josearroyo.fitlog.viewmodel.entrenador.EntrenadorViewModel
import dev.josearroyo.fitlog.viewmodel.entrenador.AsistenciaAtletaUI
import dev.josearroyo.fitlog.esCumpleanosHoy

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrenadorDashboardScreen(
    entrenadorId: String,
    onAtletaClick: (String) -> Unit,
    onAddAtletaClick: () -> Unit
) {
    val dashboardViewModel: EntrenadorViewModel = viewModel { EntrenadorViewModel() }

    // 🟢 AQUÍ ESTÁ EL CAMBIO CLAVE: Recolectamos el estado completo unificado
    val state by dashboardViewModel.uiState.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    var mostrarDialogOpciones by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(entrenadorId) {
        dashboardViewModel.cargarDashboard(entrenadorId)
    }

    Box(modifier = Modifier.fillMaxSize().background(FondoOscuro)) {
        Column(modifier = Modifier.fillMaxSize()) {

            TabRow(
                selectedTabIndex = state.tabSeleccionado,
                containerColor = FondoOscuro,
                contentColor = NaranjaAcento
            ) {
                Tab(
                    selected = state.tabSeleccionado == 0,
                    onClick = { dashboardViewModel.cambiarTab(0) },
                    text = { Text("Mis Atletas", color = Color.White, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = state.tabSeleccionado == 1,
                    onClick = { dashboardViewModel.cambiarTab(1) },
                    text = { Text("Asistencia Hoy", color = Color.White, fontWeight = FontWeight.Bold) }
                )
            }

            OutlinedTextField(
                value = state.textoBusqueda,
                onValueChange = { dashboardViewModel.aplicarBusqueda(it) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Buscar atleta por nombre...", color = TextoSecundario) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NaranjaAcento) },
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.3f),
                    focusedContainerColor = FondoTarjeta, unfocusedContainerColor = FondoTarjeta
                )
            )

            if (state.tabSeleccionado == 0) {
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NaranjaAcento)
                    }
                } else if (state.atletas.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No se encontraron atletas.", color = TextoSecundario, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(state.atletas, key = { it.id }) { atleta ->
                            AtletaCardItem(atleta = atleta, onClick = { onAtletaClick(atleta.id) })
                        }
                    }
                }
            } else {
                if (state.isLoadingAsistencia) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NaranjaAcento)
                    }
                } else if (state.asistenciaDia.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Sin registros de asistencia hoy.", color = TextoSecundario, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(state.asistenciaDia, key = { it.atleta.id }) { reporte ->
                            AsistenciaCardItem(reporte = reporte)
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (state.codigoGenerado != null) {
                    clipboardManager.setText(AnnotatedString(state.codigoGenerado!!))
                    dashboardViewModel.limpiarCodigo()
                } else {
                    mostrarDialogOpciones = true
                }
            },
            containerColor = NaranjaAcento,
            contentColor = FondoOscuro,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            if (state.isGeneratingCode) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = FondoOscuro)
            } else if (state.codigoGenerado != null) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar: ${state.codigoGenerado}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            } else {
                Icon(Icons.Default.Add, contentDescription = "Agregar Atleta")
            }
        }

        // ... (El resto del código del diálogo sigue igual)
        if (mostrarDialogOpciones) {
            AlertDialog(
                onDismissRequest = { mostrarDialogOpciones = false },
                containerColor = FondoTarjeta,
                title = { Text("Registrar Atleta", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                text = { Text("Selecciona cómo quieres registrar o vincular a tu nuevo atleta.", color = TextoSecundario, fontSize = 14.sp) },
                confirmButton = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { mostrarDialogOpciones = false; onAddAtletaClick() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento)
                        ) { Text("Crear Manualmente", color = Color.White, fontWeight = FontWeight.Bold) }

                        OutlinedButton(
                            onClick = { mostrarDialogOpciones = false; dashboardViewModel.generarCodigoVinculacion(entrenadorId) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NaranjaAcento),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NaranjaAcento)
                        ) { Text("Generar Código", fontWeight = FontWeight.Bold) }
                    }
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtletaCardItem(atleta: Usuario, onClick: () -> Unit) {
    val esCumpleanos = esCumpleanosHoy(atleta.fechaNacimiento)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(40.dp).background(FondoOscuro, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = NaranjaAcento)
                }
                if (esCumpleanos) { Text("🎂", modifier = Modifier.offset(x = 10.dp, y = (-12).dp), fontSize = 14.sp) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${atleta.nombres} ${atleta.apellidos}".trim().ifEmpty { "Atleta Sin Nombre" }, fontWeight = FontWeight.Bold, color = Color.White)
                if (esCumpleanos) {
                    Text("🎉 ¡Hoy cumple años!", style = MaterialTheme.typography.bodySmall, color = NaranjaAcento, fontWeight = FontWeight.Bold)
                } else {
                    Text("Plan: ${atleta.planActivo}", color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
                }
            }
            val colorEstado = if (atleta.estadoSuscripcion == EstadoSuscripcion.ACTIVO) Color(0xFF81C784) else Color(0xFFE57373)
            Surface(color = colorEstado.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                Text(text = atleta.estadoSuscripcion.name, color = colorEstado, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AsistenciaCardItem(reporte: AsistenciaAtletaUI) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (reporte.asistio) Icons.Default.AssignmentTurnedIn else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (reporte.asistio) Color(0xFF81C784) else Color(0xFFE57373),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "${reporte.atleta.nombres} ${reporte.atleta.apellidos}".trim(), fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = if (reporte.asistio) "Entrenó hoy a las ${reporte.horaEntrenamiento}" else "Sin asistencia registrada hoy",
                    color = if (reporte.asistio) NaranjaAcento else TextoSecundario,
                    fontSize = 13.sp
                )
            }
        }
    }
}