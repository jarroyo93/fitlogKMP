package dev.josearroyo.fitlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
// 🟢 Importamos el gestor de borrador local multiplataforma
import dev.josearroyo.fitlog.ui.util.BorradorLocalManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🟢 PASO 2: Inicializamos la caché local con el contexto de la aplicación
        BorradorLocalManager.initialize(applicationContext)

        setContent {
            // 🔥 Llamamos directamente a la función App() de commonMain
            App()
        }
    }
}