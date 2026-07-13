package dev.josearroyo.fitlog.ui.dashboard.entrenador

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.GrupoMuscular
import dev.josearroyo.fitlog.viewmodel.entrenador.AddEjercicioViewModel

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEjercicioScreen(entrenadorId: String, ejercicioId: String? = null, onBack: () -> Unit) {
    val viewModel: AddEjercicioViewModel = viewModel { AddEjercicioViewModel() }
    val isSaved by viewModel.isSaved.collectAsState()
    val nombre by viewModel.nombre.collectAsState()
    val grupo by viewModel.grupoMuscular.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(ejercicioId) { viewModel.cargarEjercicioSiExiste(ejercicioId) }
    LaunchedEffect(isSaved) { if (isSaved) onBack() }

    Scaffold(containerColor = FondoOscuro, topBar = {
        TopAppBar(title = { Text(if (ejercicioId == null) "Nuevo Ejercicio" else "Editar Ejercicio", color = Color.White) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro),
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = NaranjaAcento) } }
        )
    }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            OutlinedTextField(value = nombre, onValueChange = viewModel::actualizarNombre, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(value = grupo.name.replace("_", " "), onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor().fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    GrupoMuscular.values().forEach { g -> DropdownMenuItem(text = { Text(g.name) }, onClick = { viewModel.actualizarGrupo(g); expanded = false }) }
                }
            }
            Button(onClick = { viewModel.guardarEjercicio(entrenadorId) }, modifier = Modifier.fillMaxWidth()) { Text("Guardar") }
        }
    }
}