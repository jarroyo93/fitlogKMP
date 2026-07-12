package dev.josearroyo.fitlog.ui.entrenador

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.EstadoSuscripcion
import dev.josearroyo.fitlog.viewmodel.entrenador.AtletaDetailViewModel

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtletaDetailScreen(
    atletaId: String,
    onBack: () -> Unit,
    onNavigateToHistorialValoraciones: (String) -> Unit,
    onNavigateToHistorialHabitos: (String) -> Unit,
    onNavigateToPerfil: (String) -> Unit,
    onNavigateToRendimiento: (String) -> Unit,
    onNavigateToSeleccionarPlantilla: (String, String) -> Unit,
    onNavigateToEditRutina: (String, String) -> Unit
) {
    val detailViewModel: AtletaDetailViewModel = viewModel { AtletaDetailViewModel() }
    val state by detailViewModel.state.collectAsState()

    LaunchedEffect(atletaId) {
        detailViewModel.cargarExpedienteAtleta(atletaId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.atleta?.nombres ?: "Detalle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = NaranjaAcento)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FondoOscuro)
                .padding(innerPadding)
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NaranjaAcento)
                }
            } else if (state.error != null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.error ?: "Error desconocido",
                        color = Color(0xFFE57373),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val atleta = state.atleta!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Ficha del Atleta (Nombre y Estado de Membresía)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).background(FondoOscuro, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = NaranjaAcento, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${atleta.nombres} ${atleta.apellidos}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("ID: ${atleta.numeroDocumento}", color = TextoSecundario, fontSize = 13.sp)
                            }

                            val colorEstado = if (atleta.estadoSuscripcion == EstadoSuscripcion.ACTIVO) Color(0xFF81C784) else Color(0xFFE57373)
                            Surface(color = colorEstado.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                                Text(atleta.estadoSuscripcion.name, color = colorEstado, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 2. Estatus de su Rutina de Carga
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Panel de Control (Ciclo Activo)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(6.dp))

                            if (state.cicloActivo != null) {
                                Text("Tiene un ciclo de entrenamiento activo corriendo actualmente.", color = TextoSecundario, fontSize = 13.sp)
                            } else {
                                Text("Este alumno no cuenta con rutinas asignadas para esta semana.", color = TextoSecundario, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onNavigateToSeleccionarPlantilla(atletaId, atleta.entrenadorId ?: "") },
                                    colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Asignar Rutina", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 3. Cuadrícula de Accesos Rápidos (Los 4 Pilares del Alumno)
                    Text("Expediente General", color = Color.White, fontWeight = FontWeight.Bold)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            MenuButton(Modifier.weight(1f), "Valoración", Icons.Default.Info) { onNavigateToHistorialValoraciones(atletaId) }
                            MenuButton(Modifier.weight(1f), "Hábitos", Icons.Default.Favorite) { onNavigateToHistorialHabitos(atletaId) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            MenuButton(Modifier.weight(1f), "Perfil", Icons.Default.Search) { onNavigateToPerfil(atletaId) }
                            MenuButton(Modifier.weight(1f), "Diario Cargas", Icons.Default.Star) { onNavigateToRendimiento(atletaId) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuButton(modifier: Modifier, text: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, tint = NaranjaAcento)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}