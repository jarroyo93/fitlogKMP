package dev.josearroyo.fitlog.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.josearroyo.fitlog.data.model.RolUsuario
import dev.josearroyo.fitlog.ui.login.LoginScreen
import dev.josearroyo.fitlog.ui.login.CambiarContrasenaScreen
import dev.josearroyo.fitlog.ui.splash.SplashScreen
import dev.josearroyo.fitlog.ui.entrenador.EntrenadorMainScreen
import dev.josearroyo.fitlog.ui.entrenador.AtletaDetailScreen
import dev.josearroyo.fitlog.ui.entrenador.AddValoracionScreen
import dev.josearroyo.fitlog.ui.atleta.HistorialValoracionScreen
import dev.josearroyo.fitlog.ui.atleta.HistorialHabitosScreen
import dev.josearroyo.fitlog.ui.atleta.AddHabitosScreen
import dev.josearroyo.fitlog.ui.dashboard.PerfilAtletaScreen
import dev.josearroyo.fitlog.ui.dashboard.ProgresoAtletaScreen
import dev.josearroyo.fitlog.ui.dashboard.EditRutinaAsignadaScreen
import dev.josearroyo.fitlog.ui.dashboard.entrenador.AddEjercicioScreen
import dev.josearroyo.fitlog.ui.dashboard.entrenador.AddPlantillaScreen
import dev.josearroyo.fitlog.ui.dashboard.entrenador.BibliotecaScreen
import dev.josearroyo.fitlog.ui.profile.EditarDatosPersonalesScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = Modifier.fillMaxSize()
    ) {
        // ============================================================
        // 🔑 AUTENTICACIÓN Y SPLASH
        // ============================================================
        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                onLoginSuccess = { uid, rol ->
                    val destination = when (rol) {
                        RolUsuario.ENTRENADOR -> "dashboard_entrenador/$uid"
                        else -> "dashboard_atleta/$uid"
                    }
                    navController.navigate(destination) {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "cambiar_password/{uid}",
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            CambiarContrasenaScreen(
                uid = uid,
                onPasswordChangedSuccess = {
                    navController.navigate("dashboard_atleta/$uid") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // ============================================================
        // 🏛️ PANEL DEL ENTRENADOR (DASHBOARD CO-CENTRAL)
        // ============================================================
        composable(
            route = "dashboard_entrenador/{uid}",
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            EntrenadorMainScreen(
                uid = uid,
                onNavigateToAtletaDetail = { atletaId ->
                    navController.navigate("atleta_detail/$atletaId")
                },
                onNavigateToAddExercise = { id ->
                    navController.navigate("add_ejercicio/$id")
                },
                onNavigateToAddPlantilla = { id ->
                    navController.navigate("add_plantilla/$id")
                },
                onNavigateToEditExercise = { idEnt, idEj ->
                    navController.navigate("edit_ejercicio/$idEnt/$idEj")
                },
                onNavigateToEditPlantilla = { idEnt, idPlan ->
                    navController.navigate("edit_plantilla/$idEnt/$idPlan")
                },
                onNavigateToAddAtleta = { /* ... */ },
                onNavigateToEditarDatosPersonales = { entrenadorId ->
                    navController.navigate("editar_datos_personales/$entrenadorId")
                },
                onNavigateToHistorialFacturacion = { atletaId, _ ->
                    println("Navegación incremental KMP: Abrir historial de cobros del atleta $atletaId")
                },
                onNavigateToInformeGlobalFacturacion = { _ -> },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ============================================================
        // 📋 EXPEDIENTE Y GESTIÓN PROFUNDA DEL ALUMNO
        // ============================================================
        composable(
            route = "atleta_detail/{atletaId}",
            arguments = listOf(navArgument("atletaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val atletaId = backStackEntry.arguments?.getString("atletaId") ?: ""
            AtletaDetailScreen(
                atletaId = atletaId,
                onBack = { navController.popBackStack() },
                onNavigateToHistorialValoraciones = { id ->
                    navController.navigate("historial_valoracion/$id")
                },
                onNavigateToHistorialHabitos = { id ->
                    navController.navigate("historial_habitos/$id")
                },
                onNavigateToPerfil = { id ->
                    navController.navigate("perfil_atleta/$id")
                },
                onNavigateToRendimiento = { id ->
                    navController.navigate("progreso_atleta/$id")
                },
                onNavigateToSeleccionarPlantilla = { idAtleta, idEntrenador ->
                    println("Navegación incremental KMP: Asignar plantilla al atleta $idAtleta por el coach $idEntrenador")
                },
                onNavigateToEditRutina = { idAtleta, idRutina ->
                    navController.navigate("edit_rutina_asignada/$idAtleta/$idRutina")
                }
            )
        }

        composable(
            route = "historial_valoracion/{atletaId}",
            arguments = listOf(navArgument("atletaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val atletaId = backStackEntry.arguments?.getString("atletaId") ?: ""
            HistorialValoracionScreen(
                atletaId = atletaId,
                onBack = { navController.popBackStack() },
                onNavigateToNuevaValoracion = { id ->
                    navController.navigate("agregar_valoracion/$id")
                }
            )
        }

        composable(
            route = "agregar_valoracion/{atletaId}",
            arguments = listOf(navArgument("atletaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val atletaId = backStackEntry.arguments?.getString("atletaId") ?: ""
            AddValoracionScreen(atletaId = atletaId, onBack = { navController.popBackStack() })
        }

        composable(
            route = "historial_habitos/{atletaId}",
            arguments = listOf(navArgument("atletaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val atletaId = backStackEntry.arguments?.getString("atletaId") ?: ""
            HistorialHabitosScreen(
                atletaId = atletaId,
                onBack = { navController.popBackStack() },
                onNavigateToNuevo = { id ->
                    navController.navigate("agregar_habitos/$id")
                }
            )
        }

        composable(
            route = "agregar_habitos/{atletaId}",
            arguments = listOf(navArgument("atletaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val atletaId = backStackEntry.arguments?.getString("atletaId") ?: ""
            AddHabitosScreen(atletaId = atletaId, onBack = { navController.popBackStack() })
        }

        composable(
            route = "perfil_atleta/{atletaId}",
            arguments = listOf(navArgument("atletaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val atletaId = backStackEntry.arguments?.getString("atletaId") ?: ""
            PerfilAtletaScreen(atletaId = atletaId, onBack = { navController.popBackStack() })
        }

        composable(
            route = "edit_rutina_asignada/{atletaId}/{rutinaId}",
            arguments = listOf(
                navArgument("atletaId") { type = NavType.StringType },
                navArgument("rutinaId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val atletaId = backStackEntry.arguments?.getString("atletaId") ?: ""
            val rutinaId = backStackEntry.arguments?.getString("rutinaId") ?: ""
            EditRutinaAsignadaScreen(
                atletaId = atletaId,
                rutinaId = rutinaId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "progreso_atleta/{atletaId}",
            arguments = listOf(navArgument("atletaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val atletaId = backStackEntry.arguments?.getString("atletaId") ?: ""
            ProgresoAtletaScreen(userId = atletaId, onBack = { navController.popBackStack() })
        }

        // ============================================================
        // 📚 MÓDULO DE LA BIBLIOTECA (EJERCICIOS Y PLANTILLAS)
        // ============================================================
        composable(
            route = "biblioteca/{entrenadorId}",
            arguments = listOf(navArgument("entrenadorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("entrenadorId") ?: ""
            BibliotecaScreen(
                entrenadorId = id,
                onNavigateToAddEjercicio = { entId -> navController.navigate("add_ejercicio/$entId") },
                onNavigateToEditEjercicio = { entId, ejId -> navController.navigate("edit_ejercicio/$entId/$ejId") },
                onNavigateToAddPlantilla = { entId -> navController.navigate("add_plantilla/$entId") },
                onNavigateToEditPlantilla = { entId, planId -> navController.navigate("edit_plantilla/$entId/$planId") }
            )
        }

        composable("add_ejercicio/{entrenadorId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("entrenadorId") ?: ""
            AddEjercicioScreen(entrenadorId = id, onBack = { navController.popBackStack() })
        }

        composable("edit_ejercicio/{entrenadorId}/{ejercicioId}") { backStackEntry ->
            val entId = backStackEntry.arguments?.getString("entrenadorId") ?: ""
            val ejId = backStackEntry.arguments?.getString("ejercicioId") ?: ""
            AddEjercicioScreen(entrenadorId = entId, ejercicioId = ejId, onBack = { navController.popBackStack() })
        }

        composable("add_plantilla/{entrenadorId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("entrenadorId") ?: ""
            AddPlantillaScreen(entrenadorId = id, onBack = { navController.popBackStack() })
        }

        composable("edit_plantilla/{entrenadorId}/{plantillaId}") { backStackEntry ->
            val entId = backStackEntry.arguments?.getString("entrenadorId") ?: ""
            val planId = backStackEntry.arguments?.getString("plantillaId") ?: ""
            AddPlantillaScreen(entrenadorId = entId, plantillaId = planId, onBack = { navController.popBackStack() })
        }

        // ============================================================
        // 👥 RUTA COMPARTIDA / CAMALEÓNICA DE DATOS PERSONALES
        // ============================================================
        composable(
            route = "editar_datos_personales/{uid}",
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""

            val entrenadorVM: dev.josearroyo.fitlog.viewmodel.entrenador.PerfilEntrenadorViewModel = viewModel()
            val atletaVM: dev.josearroyo.fitlog.viewmodel.atleta.PerfilAtletaViewModel = viewModel()

            val stateEntrenador by entrenadorVM.uiState.collectAsState()
            val stateAtleta by atletaVM.uiState.collectAsState()

            LaunchedEffect(uid) {
                entrenadorVM.cargarPerfil(uid)
                atletaVM.cargarPerfil(uid)
            }

            LaunchedEffect(stateEntrenador.exitoGuardado, stateAtleta.guardadoExitoso) {
                if (stateEntrenador.exitoGuardado) {
                    entrenadorVM.resetExito()
                    navController.popBackStack()
                } else if (stateAtleta.guardadoExitoso) {
                    atletaVM.resetExito()
                    navController.popBackStack()
                }
            }

            when {
                // ... dentro del 'when' en AppNavigation.kt
                stateEntrenador.usuarioLogueado != null -> {
                    val usuario = stateEntrenador.usuarioLogueado!!
                    EditarDatosPersonalesScreen(
                        usuarioActual = usuario,
                        isSaving = stateEntrenador.isSaving,
                        error = stateEntrenador.error,
                        onBack = { navController.popBackStack() },
                        // Mapeamos los 8 parámetros de la pantalla:
                        onGuardarCambios = { nom, ape, tDoc, nDoc, tel, _, _, _ ->
                            // Ahora pasamos correctamente cada valor al ViewModel del Entrenador
                            entrenadorVM.guardarDatosPersonales(
                                uid = uid,
                                nombres = nom,
                                apellidos = ape,
                                tipoDocumento = tDoc,
                                documento = nDoc,
                                telefono = tel
                            )
                        }
                    )
                }
                stateAtleta.usuarioLogueado != null -> {
                    val atleta = stateAtleta.usuarioLogueado!!
                    EditarDatosPersonalesScreen(
                        usuarioActual = atleta,
                        isSaving = stateAtleta.isSaving,
                        error = stateAtleta.error,
                        onBack = { navController.popBackStack() },
                        onGuardarCambios = { nom, ape, tDoc, nDoc, tel, fNac, tSangre, nac ->
                            atletaVM.actualizarDatosAtleta(uid, nom, ape, tDoc, nDoc, tel, fNac, tSangre, nac)
                        }
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF241B3C)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFF9F6D))
                    }
                }
            }
        }

        // ============================================================
        // 🏋️ PANEL INTERNO DEL ATLETA (FALLBACK)
        // ============================================================
        composable(
            route = "dashboard_atleta/{uid}",
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) {
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