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
// 🚀 IMPORTANTE: Importa tu contenedor principal migrado
import dev.josearroyo.fitlog.ui.entrenador.EntrenadorMainScreen

enum class Screen {
    Splash,
    Login,
    CambiarPassword,
    DashboardEntrenador,
    DashboardAtleta
}

@Composable
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf(Screen.Splash) }
        var currentUid by remember { mutableStateOf("") }

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

            // 🚀 CORREGIDO: Inyectamos la pantalla real con sus acciones
            Screen.DashboardEntrenador -> {
                EntrenadorMainScreen(
                    uid = currentUid,
                    onNavigateToAddAtleta = { entId -> println("Navegar a Agregar Atleta para Coach: $entId") },
                    onNavigateToAtletaDetail = { atletaId -> println("Navegar a detalle del Atleta: $atletaId") },
                    onNavigateToAddExercise = { entId -> println("Navegar a Agregar Ejercicio") },
                    onNavigateToAddPlantilla = { entId -> println("Navegar a Agregar Plantilla") },
                    onNavigateToEditExercise = { entId, ejId -> println("Editar ejercicio") },
                    onNavigateToEditPlantilla = { entId, planId -> println("Editar plantilla") },
                    onNavigateToEditarDatosPersonales = { entId -> println("Editar datos personales") },
                    onNavigateToHistorialFacturacion = { atletaId, entId -> println("Ver facturas del atleta") },
                    onNavigateToInformeGlobalFacturacion = { entId -> println("Ver informe global") },
                    onLogout = {
                        // Limpiamos los estados de sesión y retornamos al Login de forma segura
                        currentUid = ""
                        currentScreen = Screen.Login
                    }
                )
            }

            // 🚀 EVITAMOS PANTALLA BLANCA EN ATLETAS: Colocamos un esqueleto estilizado temporal
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