package dev.josearroyo.fitlog

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import dev.josearroyo.fitlog.ui.login.LoginScreen
import dev.josearroyo.fitlog.ui.login.CambiarContrasenaScreen
import dev.josearroyo.fitlog.ui.splash.SplashScreen // 🔥 Importamos tu nuevo Splash

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
                // 🔥 Se ejecuta la animación y al finalizar cambia el estado al Login de inmediato
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
                // Tu pantalla de inicio de Entrenador migrada posteriormente
            }
            Screen.DashboardAtleta -> {
                // Tu pantalla de inicio de Atleta migrada posteriormente
            }
        }
    }
}