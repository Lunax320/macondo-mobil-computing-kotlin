package com.example.macondo.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.example.macondo.R

internal enum class Pestana(val ruta: String, val etiqueta: String, @get:DrawableRes val icono: Int) {
    INICIO(Routes.inicio, "Inicio", R.drawable.ic_home),
    HABITACIONES(Routes.habitaciones, "Habitaciones", R.drawable.ic_bed),
    SERVICIOS(Routes.servicios, "Servicios", R.drawable.ic_services),
    CHAT(Routes.chat, "Asistente Macondo", R.drawable.ic_chat),
    CUENTA(Routes.login, "Mi cuenta", R.drawable.ic_account);

    companion object {
        fun paraRuta(ruta: String?) = when (ruta) {
            Routes.inicio, Routes.promociones -> INICIO
            Routes.habitaciones, Routes.habitacion -> HABITACIONES
            Routes.servicios, Routes.servicio -> SERVICIOS
            Routes.chat -> CHAT
            Routes.login, Routes.registro, Routes.recuperarContrasena,
            Routes.cliente, Routes.operador, Routes.admin -> CUENTA
            else -> null
        }
    }
}

@Composable
internal fun MacondoNavigationBar(actual: Pestana, etiquetaCuenta: String, abrir: (Pestana) -> Unit) {
    val colores = MaterialTheme.colorScheme
    NavigationBar(containerColor = colores.surfaceContainer, contentColor = colores.onSurface) {
        Pestana.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == actual,
                onClick = { abrir(tab) },
                icon = {
                    Icon(painterResource(tab.icono), contentDescription = if (tab == Pestana.CUENTA) etiquetaCuenta else tab.etiqueta)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colores.onSecondaryContainer,
                    indicatorColor = colores.secondaryContainer,
                    unselectedIconColor = colores.onSurfaceVariant
                )
            )
        }
    }
}
