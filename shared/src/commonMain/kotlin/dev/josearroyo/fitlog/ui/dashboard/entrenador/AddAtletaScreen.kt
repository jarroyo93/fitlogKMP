@file:OptIn(ExperimentalMaterial3Api::class)
package dev.josearroyo.fitlog.ui.dashboard.entrenador

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.josearroyo.fitlog.data.model.MetodoComposicionCorporal
import dev.josearroyo.fitlog.data.model.NivelExperiencia
import dev.josearroyo.fitlog.data.model.TipoPlanSuscripcion
import dev.josearroyo.fitlog.data.model.ValoracionFisica
import dev.josearroyo.fitlog.data.model.Habitos
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.viewmodel.entrenador.AddAtletaViewModel
import dev.josearroyo.fitlog.viewmodel.entrenador.AddAtletaEvent
import dev.josearroyo.fitlog.viewmodel.entrenador.AddAtletaState
import dev.josearroyo.fitlog.formatearFechaCorto

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)
private val TextoPrincipal = Color(0xFFFFFFFF)

@Composable
fun AddAtletaScreen(
    viewModel: AddAtletaViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            viewModel.onEvent(AddAtletaEvent.ResetState)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Nuevo Atleta", color = TextoPrincipal, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = TextoPrincipal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro)
            )
        },
        containerColor = FondoOscuro
    ) { paddingValues ->
        // 🟢 Usamos un Column general para que los botones y errores estén FIJOS abajo
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(FondoOscuro)
                .padding(16.dp)
        ) {
            // Indicador de Progreso
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Paso ${state.currentStep} de 4", color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                LinearProgressIndicator(
                    progress = { state.currentStep / 4f },
                    modifier = Modifier.width(120.dp).height(8.dp),
                    color = NaranjaAcento,
                    trackColor = TextoSecundario.copy(alpha = 0.2f)
                )
            }

            // Área central scrollable que contiene los subformularios
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (state.currentStep) {
                        1 -> FormularioDatosPersonales(state.usuario, state.confirmarCorreo, viewModel)
                        2 -> FormularioValoracionFisica(state.valoracionFisica, viewModel)
                        3 -> FormularioHabitos(state.habitos, viewModel)
                        4 -> FormularioSuscripcion(state, viewModel)
                    }
                }
            }

            // 🟢 ERROR FIJO ABAJO: Siempre visible sin importar el scroll
            state.error?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2B8B5).copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF2B8B5)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = err,
                        color = Color(0xFFF2B8B5),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de Navegación del Formulario (Fijos abajo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.currentStep > 1) {
                    Button(
                        onClick = { viewModel.onEvent(AddAtletaEvent.PrevStep) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = FondoTarjeta)
                    ) {
                        Text("Atrás", color = TextoPrincipal)
                    }
                }

                Button(
                    onClick = {
                        if (state.currentStep < 4) {
                            viewModel.onEvent(AddAtletaEvent.NextStep)
                        } else {
                            viewModel.onEvent(AddAtletaEvent.SaveAtleta)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (state.currentStep == 4) "Guardar Atleta" else "Siguiente", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ===================================================================================
// SUBFORMULARIOS Y COMPOSABLES AUXILIARES
// ===================================================================================

@Composable
fun FormularioDatosPersonales(u: Usuario, confirmarCorreo: String, viewModel: AddAtletaViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Datos Personales Básicos", style = MaterialTheme.typography.titleMedium, color = NaranjaAcento, fontWeight = FontWeight.Bold)

            AtletaTextField(u.nombres, { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(nombres = it))) }, "Nombres *")
            AtletaTextField(u.apellidos, { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(apellidos = it))) }, "Apellidos *")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AtletaDropdown(
                    selectedOption = u.tipoDocumento,
                    onOptionSelected = { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(tipoDocumento = it))) },
                    options = listOf("CC", "TI", "CE", "Pasaporte"),
                    label = "Tipo Doc.",
                    modifier = Modifier.weight(1.2f)
                )
                AtletaTextField(
                    u.numeroDocumento,
                    { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(numeroDocumento = it))) },
                    "Número Doc. *",
                    modifier = Modifier.weight(2f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            AtletaDatePickerFieldKmp(
                value = u.fechaNacimiento,
                onDateSelected = { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(fechaNacimiento = it))) },
                label = "Fecha de Nacimiento"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AtletaDropdown(
                    selectedOption = u.tipoSangre,
                    onOptionSelected = { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(tipoSangre = it))) },
                    options = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"),
                    label = "T. Sangre",
                    modifier = Modifier.weight(1f)
                )
                // 🟢 REEMPLAZADO por el dropdown con buscador de nacionalidades
                SearchableNacionalidadDropdown(
                    selectedNacionalidad = u.nacionalidad,
                    onNacionalidadSelected = { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(nacionalidad = it))) },
                    modifier = Modifier.weight(1.5f)
                )
            }

            AtletaTextField(u.telefono, { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(telefono = it))) }, "Teléfono Celular", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            AtletaTextField(u.correo, { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(correo = it))) }, "Correo Electrónico *", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))

            // 🟢 Campo con aviso visual de no coincidencia en tiempo real
            val noCoincide = u.correo.isNotBlank() && confirmarCorreo.isNotBlank() && u.correo.trim().lowercase() != confirmarCorreo.trim().lowercase()
            AtletaTextField(
                value = confirmarCorreo,
                onValueChange = { viewModel.onEvent(AddAtletaEvent.UpdateConfirmarCorreo(it)) },
                label = "Confirmar Correo Electrónico *",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            if (noCoincide) {
                Text(
                    text = "Los correos electrónicos no coinciden",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun FormularioValoracionFisica(v: ValoracionFisica, viewModel: AddAtletaViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Valoración Física Inicial", style = MaterialTheme.typography.titleMedium, color = NaranjaAcento, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecimalField(v.pesoKg, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(pesoKg = it))) }, "Peso (kg)", Modifier.weight(1f))
                DecimalField(v.alturaCm, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(alturaCm = it))) }, "Altura (cm)", Modifier.weight(1f))
            }

            // 🟢 SWITCH DE COMPOSICIÓN AVANZADA ORIGINAL SIN DROPDOWN INNECESARIO
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mediciones Corporales Avanzadas", color = TextoPrincipal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Activa antropometría, bioimpedancia o ambas", color = TextoSecundario, fontSize = 12.sp)
                }
                Switch(
                    checked = v.mostrarComposicionAvanzada,
                    onCheckedChange = { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(mostrarComposicionAvanzada = it))) },
                    colors = SwitchDefaults.colors(checkedThumbColor = NaranjaAcento, checkedTrackColor = NaranjaAcento.copy(alpha = 0.4f))
                )
            }

            // 🟢 SI SE ENCUENTRA ACTIVO, MOSTRAMOS LAS 3 PESTAÑAS (TABS)
            AnimatedVisibility(visible = v.mostrarComposicionAvanzada) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val tabs = listOf(
                        MetodoComposicionCorporal.ANTROPOMETRIA to "Antropometría",
                        MetodoComposicionCorporal.BIOIMPEDANCIA to "Bioimpedancia",
                        MetodoComposicionCorporal.AMBOS to "Ambos"
                    )
                    val selectedIndex = tabs.indexOfFirst { it.first == v.metodoComposicion }.coerceAtLeast(0)

                    TabRow(
                        selectedTabIndex = selectedIndex,
                        containerColor = FondoOscuro,
                        contentColor = NaranjaAcento,
                        modifier = Modifier.fillMaxWidth().background(FondoOscuro, RoundedCornerShape(8.dp))
                    ) {
                        tabs.forEachIndexed { index, pair ->
                            Tab(
                                selected = selectedIndex == index,
                                onClick = {
                                    viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(metodoComposicion = pair.first)))
                                },
                                text = { Text(pair.second, color = if (selectedIndex == index) NaranjaAcento else TextoSecundario, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            )
                        }
                    }

                    // 🟢 SEGÚN LA PESTAÑA, CARGAMOS EL COMPONENTE ADECUADO (O AMBOS JUNTOS)
                    when (v.metodoComposicion) {
                        MetodoComposicionCorporal.ANTROPOMETRIA -> {
                            SubFormularioAntropometriaAtleta(v, viewModel)
                        }
                        MetodoComposicionCorporal.BIOIMPEDANCIA -> {
                            SubFormularioBioimpedanciaAtleta(v, viewModel)
                        }
                        MetodoComposicionCorporal.AMBOS -> {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                SubFormularioAntropometriaAtleta(v, viewModel)
                                HorizontalDivider(color = TextoSecundario.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                                SubFormularioBioimpedanciaAtleta(v, viewModel)
                            }
                        }
                    }
                }
            }

            AtletaDropdown(
                selectedOption = v.nivelExperiencia.name,
                onOptionSelected = { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(nivelExperiencia = NivelExperiencia.valueOf(it)))) },
                options = NivelExperiencia.values().map { it.name },
                label = "Nivel de Experiencia"
            )

            AtletaTextField(v.observacionesLadoIzquierdo, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(observacionesLadoIzquierdo = it))) }, "Observaciones / Notas Diagnósticas")
        }
    }
}

@Composable
fun SubFormularioAntropometriaAtleta(v: ValoracionFisica, viewModel: AddAtletaViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Perímetros Antropométricos (cm)", style = MaterialTheme.typography.labelSmall, color = NaranjaAcento)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DecimalFieldNulable(v.abdomen1, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(abdomen1 = it))) }, "Abdomen 1", Modifier.weight(1f))
            DecimalFieldNulable(v.abdomen2, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(abdomen2 = it))) }, "Abdomen 2", Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DecimalFieldNulable(v.brazoRelajado, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(brazoRelajado = it))) }, "Brazo Relajado", Modifier.weight(1f))
            DecimalFieldNulable(v.brazoFlexionado, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(brazoFlexionado = it))) }, "Brazo Flexionado", Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DecimalFieldNulable(v.gluteo, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(gluteo = it))) }, "Glúteo", Modifier.weight(1f))
            DecimalFieldNulable(v.musloProminente, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(musloProminente = it))) }, "Muslo Prom.", Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DecimalFieldNulable(v.piernaMedial, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(piernaMedial = it))) }, "Pierna Medial", Modifier.weight(1f))
            DecimalFieldNulable(v.pantorrilla, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(pantorrilla = it))) }, "Pantorrilla", Modifier.weight(1f))
        }
    }
}

@Composable
fun SubFormularioBioimpedanciaAtleta(v: ValoracionFisica, viewModel: AddAtletaViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Métricas Analíticas de Báscula Inteligente", style = MaterialTheme.typography.labelSmall, color = NaranjaAcento)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DecimalFieldNulable(v.porcentajeGrasaCorporal, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(porcentajeGrasaCorporal = it))) }, "% Grasa Corp.", Modifier.weight(1f))
            DecimalFieldNulable(v.masaMuscularKg, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(masaMuscularKg = it))) }, "Masa Musc. (kg)", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntFieldNulable(v.grasaVisceral, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(grasaVisceral = it))) }, "Grasa Visceral", Modifier.weight(1f))
            DecimalFieldNulable(v.aguaCorporalPorcentaje, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(aguaCorporalPorcentaje = it))) }, "% Agua Corp.", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntFieldNulable(v.edadMetabolica, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(edadMetabolica = it))) }, "Edad Metabólica", Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun FormularioHabitos(h: Habitos, viewModel: AddAtletaViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Estilo de Vida y Hábitos", style = MaterialTheme.typography.titleMedium, color = NaranjaAcento, fontWeight = FontWeight.Bold)

            AtletaTextField(h.actividadesPrincipales, { viewModel.onEvent(AddAtletaEvent.UpdateHabitos(h.copy(actividadesPrincipales = it))) }, "Actividades Laborales / Diarias")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AtletaTimePickerFieldKmp(h.horaDespertar, { viewModel.onEvent(AddAtletaEvent.UpdateHabitos(h.copy(horaDespertar = it))) }, "Despertar", Modifier.weight(1f))
                AtletaTimePickerFieldKmp(h.horaDormir, { viewModel.onEvent(AddAtletaEvent.UpdateHabitos(h.copy(horaDormir = it))) }, "Dormir", Modifier.weight(1f))
            }

            DecimalField(h.horasSueno, { viewModel.onEvent(AddAtletaEvent.UpdateHabitos(h.copy(horasSueno = it))) }, "Horas Promedio Sueño")
            AtletaTextField(h.diasDisponibles, { viewModel.onEvent(AddAtletaEvent.UpdateHabitos(h.copy(diasDisponibles = it))) }, "Días Disponibles para Entrenar")
            AtletaTextField(h.horarioEntrenamiento, { viewModel.onEvent(AddAtletaEvent.UpdateHabitos(h.copy(horarioEntrenamiento = it))) }, "Franja Horaria de Entrenamiento")
            IntField(h.tiempoDisponibleMinutos, { viewModel.onEvent(AddAtletaEvent.UpdateHabitos(h.copy(tiempoDisponibleMinutos = it))) }, "Tiempo Disponible por Sesión (min)")
        }
    }
}

@Composable
fun FormularioSuscripcion(state: AddAtletaState, viewModel: AddAtletaViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Asignación de Membresía Inicial", style = MaterialTheme.typography.titleMedium, color = NaranjaAcento, fontWeight = FontWeight.Bold)

            AtletaDropdown(
                selectedOption = state.planSeleccionado.etiqueta,
                onOptionSelected = { etiqueta ->
                    val plan = TipoPlanSuscripcion.values().find { it.etiqueta == etiqueta } ?: TipoPlanSuscripcion.MENSUAL
                    viewModel.onEvent(AddAtletaEvent.UpdatePlan(plan))
                },
                options = TipoPlanSuscripcion.values().map { it.etiqueta },
                label = "Membresía / Suscripción"
            )

            AnimatedVisibility(visible = state.planSeleccionado == TipoPlanSuscripcion.PERSONALIZADO) {
                IntField(state.diasPersonalizados, { viewModel.onEvent(AddAtletaEvent.UpdateDiasPersonalizados(it)) }, "Duración en Días")
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Switch(
                    checked = state.iniciarPeriodoEnseguida,
                    onCheckedChange = { viewModel.onEvent(AddAtletaEvent.UpdateIniciarPeriodo(it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = NaranjaAcento, checkedTrackColor = NaranjaAcento.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Activar Periodo de Inmediato", color = TextoPrincipal, fontSize = 14.sp)
                    Text(
                        if (state.iniciarPeriodoEnseguida) "Comienza a facturar hoy mismo" else "Quedará diferido para activación futura",
                        color = TextoSecundario,
                        fontSize = 12.sp
                    )
                }
            }

            // 🟢 SE DEVELA EL SELECTOR DE FECHA CUANDO SE DESACTIVA "Iniciar Enseguida"
            AnimatedVisibility(visible = !state.iniciarPeriodoEnseguida) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Establecer Fecha de Activación Diferida", color = TextoPrincipal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    AtletaDatePickerFieldKmp(
                        value = state.fechaInicioPlan,
                        onDateSelected = { viewModel.onEvent(AddAtletaEvent.UpdateFechaInicioPlan(it)) },
                        label = "Fecha de Inicio del Periodo"
                    )
                }
            }
        }
    }
}

// ===================================================================================
// COMPONENTES REUTILIZABLES COMUNES
// ===================================================================================

@Composable
fun AtletaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextoSecundario, fontSize = 13.sp) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NaranjaAcento,
            unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
            focusedContainerColor = FondoOscuro,
            unfocusedContainerColor = FondoOscuro,
            focusedTextColor = TextoPrincipal,
            unfocusedTextColor = TextoPrincipal
        )
    )
}

@Composable
fun AtletaDropdown(
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    options: List<String>,
    label: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = TextoSecundario, fontSize = 13.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                focusedContainerColor = FondoOscuro,
                unfocusedContainerColor = FondoOscuro,
                focusedTextColor = TextoPrincipal,
                unfocusedTextColor = TextoPrincipal
            )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = !expanded }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(FondoTarjeta)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = TextoPrincipal) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// 🟢 SE ADICIONA COMPONENTE BUSCADOR PARA LAS NACIONALIDADES (KMP SAFE)
@Composable
fun SearchableNacionalidadDropdown(
    selectedNacionalidad: String,
    onNacionalidadSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedNacionalidad,
            onValueChange = {},
            readOnly = true,
            label = { Text("Nacionalidad", color = TextoSecundario, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                focusedContainerColor = FondoOscuro,
                unfocusedContainerColor = FondoOscuro,
                focusedTextColor = TextoPrincipal,
                unfocusedTextColor = TextoPrincipal
            )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        SearchableNacionalidadDialog(
            onDismiss = { showDialog = false },
            onSelected = onNacionalidadSelected
        )
    }
}

@Composable
fun SearchableNacionalidadDialog(
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredList = remember(query) {
        listaCompletaNacionalidades.filter { it.contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FondoTarjeta,
        title = { Text("Seleccionar Nacionalidad", color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar nacionalidad...", color = TextoSecundario) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NaranjaAcento,
                        unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                        focusedTextColor = TextoPrincipal,
                        unfocusedTextColor = TextoPrincipal
                    )
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (filteredList.isEmpty()) {
                        Text("No se encontraron resultados.", color = TextoSecundario, modifier = Modifier.align(Alignment.Center))
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredList.size) { index ->
                                val nacionalidad = filteredList[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelected(nacionalidad)
                                            onDismiss()
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp)
                                ) {
                                    Text(nacionalidad, color = TextoPrincipal, fontSize = 16.sp)
                                }
                                if (index < filteredList.size - 1) {
                                    HorizontalDivider(color = TextoSecundario.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = NaranjaAcento)
            }
        }
    )
}

@Composable
fun AtletaDatePickerFieldKmp(
    value: Long,
    onDateSelected: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = if (value == 0L) "" else formatearFechaCorto(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = TextoSecundario, fontSize = 13.sp) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Seleccionar fecha",
                    tint = NaranjaAcento
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                focusedContainerColor = FondoOscuro,
                unfocusedContainerColor = FondoOscuro,
                focusedTextColor = TextoPrincipal,
                unfocusedTextColor = TextoPrincipal
            )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDatePicker = true }
        )
    }

    if (showDatePicker) {
        KmpDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = onDateSelected
        )
    }
}

@Composable
fun AtletaTimePickerFieldKmp(
    value: String,
    onTimeSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = TextoSecundario, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                focusedContainerColor = FondoOscuro,
                unfocusedContainerColor = FondoOscuro,
                focusedTextColor = TextoPrincipal,
                unfocusedTextColor = TextoPrincipal
            )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showTimePicker = true }
        )
    }

    if (showTimePicker) {
        KmpTimePickerDialog(
            onDismiss = { showTimePicker = false },
            onTimeSelected = onTimeSelected
        )
    }
}

// Diálogo Multiplataforma nativo para la fecha de nacimiento usando Material 3
@Composable
fun KmpDatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                onDismiss()
            }) {
                Text("Confirmar", color = NaranjaAcento)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextoSecundario)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = FondoTarjeta)
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                titleContentColor = TextoPrincipal,
                headlineContentColor = TextoPrincipal,
                selectedDayContainerColor = NaranjaAcento,
                selectedDayContentColor = Color.White,
                todayContentColor = NaranjaAcento
            )
        )
    }
}

// Diálogo Simple Multiplataforma para Horas
@Composable
fun KmpTimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    var hour by remember { mutableStateOf(6) }
    var minute by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FondoTarjeta,
        title = { Text("Seleccionar Hora", color = TextoPrincipal, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberPickerSimple(value = hour, onValueChange = { hour = it }, range = 0..23, label = "Hora")
                Text(" : ", color = TextoPrincipal, fontSize = 24.sp, modifier = Modifier.padding(horizontal = 16.dp))
                NumberPickerSimple(value = minute, onValueChange = { minute = it }, range = 0..59, label = "Minuto")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val formattedTime = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
                onTimeSelected(formattedTime)
                onDismiss()
            }) {
                Text("Confirmar", color = NaranjaAcento)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextoSecundario)
            }
        }
    )
}

@Composable
fun NumberPickerSimple(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextoSecundario, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > range.first) onValueChange(value - 1) else onValueChange(range.last) }) {
                Text("-", color = NaranjaAcento, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = value.toString().padStart(2, '0'),
                color = TextoPrincipal,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = { if (value < range.last) onValueChange(value + 1) else onValueChange(range.first) }) {
                Text("+", color = NaranjaAcento, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Helpers numéricos del formulario
@Composable fun DecimalField(value: Double, onValueChange: (Double) -> Unit, label: String, modifier: Modifier = Modifier) {
    AtletaTextField(if (value == 0.0) "" else value.toString(), { onValueChange(it.toDoubleOrNull() ?: 0.0) }, label, modifier, KeyboardOptions(keyboardType = KeyboardType.Decimal))
}
@Composable fun DecimalFieldNulable(value: Double?, onValueChange: (Double?) -> Unit, label: String, modifier: Modifier = Modifier) {
    AtletaTextField(value?.toString() ?: "", { onValueChange(it.toDoubleOrNull()) }, label, modifier, KeyboardOptions(keyboardType = KeyboardType.Decimal))
}
@Composable fun IntField(value: Int, onValueChange: (Int) -> Unit, label: String, modifier: Modifier = Modifier) {
    AtletaTextField(if (value == 0) "" else value.toString(), { onValueChange(it.toIntOrNull() ?: 0) }, label, modifier, KeyboardOptions(keyboardType = KeyboardType.Number))
}
@Composable fun IntFieldNulable(value: Int?, onValueChange: (Int?) -> Unit, label: String, modifier: Modifier = Modifier) {
    AtletaTextField(value?.toString() ?: "", { onValueChange(it.toIntOrNull()) }, label, modifier, KeyboardOptions(keyboardType = KeyboardType.Number))
}

// ===================================================================================
// LISTA COMPLETA DE NACIONALIDADES PARA EL BUSCADOR
// ===================================================================================
private val listaCompletaNacionalidades = listOf(
    "Alemana", "Argentina", "Australiana", "Belga", "Boliviana", "Brasileña", "Canadiense",
    "Chilena", "China", "Colombiana", "Costarricense", "Cubana", "Ecuatoriana", "Salvadoreña",
    "Española", "Estadounidense", "Francesa", "Guatemalteca", "Hondureña", "Inglesa", "Italiana",
    "Mexicana", "Nicaragüense", "Panameña", "Paraguaya", "Peruana", "Portorriqueña", "Dominicana",
    "Uruguaya", "Venezolana", "Otra"
)