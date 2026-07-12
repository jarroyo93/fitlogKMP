package dev.josearroyo.fitlog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import dev.josearroyo.fitlog.ui.login.LoginScreen
import dev.josearroyo.fitlog.ui.login.CambiarContrasenaScreen
import dev.josearroyo.fitlog.ui.splash.SplashScreen
import dev.josearroyo.fitlog.ui.entrenador.EntrenadorMainScreen
import dev.josearroyo.fitlog.ui.entrenador.AtletaDetailScreen
import dev.josearroyo.fitlog.ui.entrenador.AddValoracionScreen
// 🚀 IMPORTANTE: Importamos el historial desde su paquete correspondiente (atleta)
import dev.josearroyo.fitlog.ui.atleta.HistorialValoracionScreen

enum class Screen {
    Splash,
    Login,
    CambiarPassword,
    DashboardEntrenador,
    DashboardAtleta,
    DetalleAtleta,
    HistorialValoracion, // 🚀 1. Agregado el estado del historial al Enum
    AgregarValoracion
}

@Composable
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf(Screen.Splash) }
        var currentUid by remember { mutableStateOf("") }
        var selectedAtletaId by remember { mutableStateOf("") }

        when (currentScreen) {
            Screen.Splash -> {
                SplashScreen(
                    onSplashFinished = {
                        currentScreen = Screen.Login
                    }
                )
            }
            Screen.Login -> {
                LoginScreen(
                    onLoginSuccess = { uid, rol ->
                        currentUid = uid
                        if (rol == dev.josearroyo.fitlog.data.model.RolUsuario.ENTRENADOR) {
                            currentScreen = Screen.DashboardEntrenador
                        } else {
                            currentScreen = Screen.DashboardAtleta
                        }
                    }
                )
            }
            Screen.CambiarPassword -> {
                CambiarContrasenaScreen(
                    uid = currentUid,
                    onPasswordChangedSuccess = {
                        currentScreen = Screen.DashboardAtleta
                    }
                )
            }

            Screen.DashboardEntrenador -> {
                EntrenadorMainScreen(
                    uid = currentUid,
                    onNavigateToAtletaDetail = { atletaId ->
                        selectedAtletaId = atletaId
                        currentScreen = Screen.DetalleAtleta
                    },
                    onNavigateToAddAtleta = { /* ... */ },
                    onNavigateToAddExercise = { /* ... */ },
                    onNavigateToAddPlantilla = { /* ... */ },
                    onNavigateToEditExercise = { _, _ -> },
                    onNavigateToEditPlantilla = { _, _ -> },
                    onNavigateToEditarDatosPersonales = { _ -> },
                    onNavigateToHistorialFacturacion = { _, _ -> },
                    onNavigateToInformeGlobalFacturacion = { _ -> },
                    onLogout = {
                        currentUid = ""
                        currentScreen = Screen.Login
                    }
                )
            }

            Screen.DetalleAtleta -> {
                AtletaDetailScreen(
                    atletaId = selectedAtletaId,
                    onBack = {
                        currentScreen = Screen.DashboardEntrenador
                    },
                    onNavigateToHistorialValoraciones = { id ->
                        // 🚀 2. CORREGIDO: Al pulsar valoración, viajamos primero al historial
                        selectedAtletaId = id
                        currentScreen = Screen.HistorialValoracion
                    },
                    onNavigateToHistorialHabitos = { id ->
                        println("Navegación incremental KMP: Historial de Hábitos para atleta ID: $id")
                    },
                    onNavigateToPerfil = { id ->
                        println("Navegación incremental KMP: Ver Perfil completo del atleta ID: $id")
                    },
                    onNavigateToRendimiento = { id ->
                        println("Navegación incremental KMP: Ver Diario de Cargas del atleta ID: $id")
                    },
                    onNavigateToSeleccionarPlantilla = { idAtleta, idEntrenador ->
                        println("Navegación incremental KMP: Asignar plantilla al atleta $idAtleta por el coach $idEntrenador")
                    },
                    onNavigateToEditRutina = { idAtleta, idRutina ->
                        println("Navegación incremental KMP: Editar rutina asignada ID: $idRutina del atleta $idAtleta")
                    }
                )
            }

            // 🚀 3. NUEVA RAMA: Agregamos el renderizado del Historial de Valoraciones
            Screen.HistorialValoracion -> {
                HistorialValoracionScreen(
                    atletaId = selectedAtletaId,
                    onBack = {
                        // Al darle atrás en el historial, regresamos al Expediente del Atleta
                        currentScreen = Screen.DetalleAtleta
                    },
                    onNavigateToNuevaValoracion = { id ->
                        // Al pulsar el botón flotante (+), viajamos al formulario de creación
                        selectedAtletaId = id
                        currentScreen = Screen.AgregarValoracion
                    }
                )
            }

            Screen.AgregarValoracion -> {
                AddValoracionScreen(
                    atletaId = selectedAtletaId,
                    onBack = {
                        // 🚀 4. CORREGIDO: Al guardar o dar atrás, regresamos al Historial (para ver el nuevo registro), no al detalle
                        currentScreen = Screen.HistorialValoracion
                    }
                )
            }

            Screen.DashboardAtleta -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF241B3C)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Panel del Atleta\n(Próxima migración modular)",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}