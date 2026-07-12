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

    // 🚀 RECOLECCIÓN DE LOS FLUJOS INDIVIDUALES:
    val atletas by dashboardViewModel.atletas.collectAsState()
    val isLoading by dashboardViewModel.isLoading.collectAsState()
    val codigoGenerado by dashboardViewModel.codigoGenerado.collectAsState()
    val expiracionCodigoTexto by dashboardViewModel.expiracionCodigoTexto.collectAsState()
    val isGeneratingCode by dashboardViewModel.isGeneratingCode.collectAsState()
    val asistenciaDia by dashboardViewModel.asistenciaDia.collectAsState()
    val isLoadingAsistencia by dashboardViewModel.isLoadingAsistencia.collectAsState()
    val textoBusqueda by dashboardViewModel.textoBusqueda.collectAsState()
    val tabSeleccionado by dashboardViewModel.tabSeleccionado.collectAsState()

    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(entrenadorId) {
        dashboardViewModel.cargarDashboard(entrenadorId)
    }

    Box(modifier = Modifier.fillMaxSize().background(FondoOscuro)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. Pestañas Superiores (Mis Atletas vs Asistencia Hoy)
            TabRow(
                selectedTabIndex = tabSeleccionado,
                containerColor = FondoOscuro,
                contentColor = NaranjaAcento
            ) {
                Tab(
                    selected = tabSeleccionado == 0,
                    onClick = { dashboardViewModel.cambiarTab(0) },
                    text = { Text("Mis Atletas", color = Color.White, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = tabSeleccionado == 1,
                    onClick = { dashboardViewModel.cambiarTab(1) },
                    text = { Text("Asistencia Hoy", color = Color.White, fontWeight = FontWeight.Bold) }
                )
            }

            // 2. Buscador Central
            OutlinedTextField(
                value = textoBusqueda,
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

            // 3. Renderizado Condicional
            if (tabSeleccionado == 0) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NaranjaAcento)
                    }
                } else if (atletas.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No se encontraron atletas.", color = TextoSecundario, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(atletas) { atleta ->
                            AtletaCardItem(atleta = atleta, onClick = { onAtletaClick(atleta.id) })
                        }
                    }
                }
            } else {
                if (isLoadingAsistencia) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NaranjaAcento)
                    }
                } else if (asistenciaDia.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Sin registros de asistencia hoy.", color = TextoSecundario, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(asistenciaDia) { reporte ->
                            AsistenciaCardItem(reporte = reporte)
                        }
                    }
                }
            }
        }

        // 4. Botón Flotante para Generar Código (FAB)
        FloatingActionButton(
            onClick = {
                if (codigoGenerado == null) {
                    dashboardViewModel.generarCodigoVinculacion(entrenadorId)
                } else {
                    clipboardManager.setText(AnnotatedString(codigoGenerado!!))
                    dashboardViewModel.limpiarCodigo()
                }
            },
            containerColor = NaranjaAcento,
            contentColor = FondoOscuro,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            if (isGeneratingCode) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = FondoOscuro)
            } else if (codigoGenerado != null) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar: $codigoGenerado", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            } else {
                Icon(Icons.Default.Add, contentDescription = "Generar Código")
            }
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