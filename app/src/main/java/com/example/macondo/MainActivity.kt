package com.example.macondo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.example.macondo.data.repository.AccountRepository
import com.example.macondo.data.repository.AdminRepository
import com.example.macondo.data.repository.OperationsRepository
import com.example.macondo.data.repository.CatalogRepository
import com.example.macondo.data.repository.ReservationRepository
import com.example.macondo.data.remote.DescubrimientoApi
import com.example.macondo.data.remote.ChatApi
import com.example.macondo.data.repository.PromocionesRepository
import com.example.macondo.ui.navigation.MacondoApp
import com.example.macondo.ui.theme.Apariencia
import com.example.macondo.ui.theme.MacondoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var catalogo: CatalogRepository
    @Inject lateinit var cuentas: AccountRepository
    @Inject lateinit var reservas: ReservationRepository
    @Inject lateinit var operaciones: OperationsRepository
    @Inject lateinit var admin: AdminRepository
    @Inject lateinit var descubrimiento: DescubrimientoApi
    @Inject lateinit var promociones: PromocionesRepository
    @Inject lateinit var chat: ChatApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferencias = getSharedPreferences("preferencias_macondo", MODE_PRIVATE)
        val aparienciaGuardada = Apariencia.entries.firstOrNull {
            it.name == preferencias.getString("apariencia", null)
        } ?: Apariencia.SISTEMA
        setContent {
            var apariencia by remember { mutableStateOf(aparienciaGuardada) }
            val oscuro = when (apariencia) {
                Apariencia.SISTEMA -> isSystemInDarkTheme()
                Apariencia.CLARA -> false
                Apariencia.OSCURA -> true
            }
            SideEffect {
                val barras = WindowCompat.getInsetsController(window, window.decorView)
                barras.isAppearanceLightStatusBars = !oscuro
                barras.isAppearanceLightNavigationBars = !oscuro
            }
            MacondoTheme(oscuro) {
                MacondoApp(catalogo, cuentas, reservas, operaciones, admin, descubrimiento, promociones, chat, apariencia) {
                    apariencia = it
                    preferencias.edit().putString("apariencia", it.name).apply()
                }
            }
        }
    }
}
