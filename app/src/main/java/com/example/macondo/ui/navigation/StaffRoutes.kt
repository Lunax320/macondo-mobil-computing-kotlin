package com.example.macondo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.macondo.data.Sesion
import com.example.macondo.data.repository.AdminRepository
import com.example.macondo.data.repository.CatalogRepository
import com.example.macondo.data.repository.OperationsRepository
import com.example.macondo.data.repository.PromocionesRepository
import com.example.macondo.ui.components.Page
import com.example.macondo.ui.components.Primary
import com.example.macondo.ui.screens.*

internal fun NavGraphBuilder.staffRoutes(
    nav: NavController,
    sesion: () -> Sesion?,
    operaciones: OperationsRepository,
    admin: AdminRepository,
    catalogo: CatalogRepository,
    promociones: PromocionesRepository,
    promocionesDisponibles: () -> Boolean,
    altaOperadoresDisponible: () -> Boolean,
    cerrarSesion: () -> Unit
) {
    composable(Routes.operador) {
        StaffDest(sesion(), nav) {
            OperadorScreen({ nav.navigate(Routes.operadorReservas) }, { nav.navigate(Routes.buscarCuenta) },
                { nav.navigate(Routes.checkout()) }, cerrarSesion)
        }
    }
    composable(Routes.operadorReservas) {
        StaffDest(sesion(), nav) {
            OperadorReservasScreen(operaciones) { nav.navigate(Routes.operadorDetalle(it)) }
        }
    }
    composable(Routes.operadorDetalle, arguments = listOf(navArgument("numeroReserva") { type = NavType.StringType })) { entrada ->
        StaffDest(sesion(), nav) {
            val numero = requireNotNull(entrada.arguments?.getString("numeroReserva"))
            OperadorDetalleScreen(operaciones, numero,
                { nav.navigate(Routes.cuenta(it)) }, { nav.navigate(Routes.checkout(it)) })
        }
    }
    composable(Routes.buscarCuenta) {
        StaffDest(sesion(), nav) {
            BuscarCuentaScreen(operaciones, checkout = false) { nav.navigate(Routes.cuenta(it)) }
        }
    }
    composable(Routes.cuenta, arguments = listOf(idArgument("reservaId"))) { entrada ->
        StaffDest(sesion(), nav) {
            CuentaScreen(operaciones, entrada.id("reservaId"), checkout = false) { nav.popBackStack() }
        }
    }
    composable(Routes.checkout, arguments = listOf(idArgument("reservaId", opcional = true))) { entrada ->
        StaffDest(sesion(), nav) {
            val id = entrada.id("reservaId")
            if (id > 0) {
                CuentaScreen(operaciones, id, checkout = true) {
                    nav.navigate(Routes.operadorReservas) { popUpTo(Routes.operador); launchSingleTop = true }
                }
            } else {
                BuscarCuentaScreen(operaciones, checkout = true) { nav.navigate(Routes.checkout(it)) }
            }
        }
    }

    composable(Routes.admin) {
        StaffDest(sesion(), nav, administrador = true) {
            AdminScreen(admin, { nav.navigate(Routes.adminHabitaciones) }, { nav.navigate(Routes.adminTipos) },
                { nav.navigate(Routes.adminServicios) }, { nav.navigate(Routes.adminOperadores) },
                { nav.navigate(Routes.operador) },
                if (promocionesDisponibles()) ({ nav.navigate(Routes.adminPromociones) }) else null,
                cerrarSesion)
        }
    }
    composable(Routes.adminPromociones) {
        StaffDest(sesion(), nav, administrador = true) {
            if (promocionesDisponibles()) {
                AdminPromocionesScreen(promociones) { nav.navigate(Routes.adminPromocionForm(it)) }
            } else Page("Promociones") { Text("La administración de promociones no está disponible.") }
        }
    }
    composable(Routes.adminPromocionForm, arguments = listOf(idArgument("id", opcional = true))) { entrada ->
        StaffDest(sesion(), nav, administrador = true) {
            if (promocionesDisponibles()) {
                AdminPromocionFormScreen(promociones, entrada.id().takeIf { it > 0 }) {
                    nav.reiniciarListado(Routes.adminPromociones)
                }
            } else Page("Promociones") { Text("La administración de promociones no está disponible.") }
        }
    }
    composable(Routes.adminHabitaciones) {
        StaffDest(sesion(), nav, administrador = true) {
            AdminHabitacionesScreen(admin, { nav.navigate(Routes.adminEditarHabitacion(it)) },
                { nav.navigate(Routes.adminNuevaHabitacion) })
        }
    }
    composable(Routes.adminNuevaHabitacion) {
        StaffDest(sesion(), nav, administrador = true) {
            AdminHabitacionFormScreen(admin, catalogo, null) { nav.reiniciarListado(Routes.adminHabitaciones) }
        }
    }
    composable(Routes.adminEditarHabitacion, arguments = listOf(idArgument("id"))) { entrada ->
        StaffDest(sesion(), nav, administrador = true) {
            AdminHabitacionFormScreen(admin, catalogo, entrada.id()) { nav.reiniciarListado(Routes.adminHabitaciones) }
        }
    }
    composable(Routes.adminTipos) {
        StaffDest(sesion(), nav, administrador = true) { AdminTiposScreen(admin, catalogo) }
    }
    composable(Routes.adminServicios) {
        StaffDest(sesion(), nav, administrador = true) {
            AdminServiciosScreen(admin, { nav.navigate(Routes.adminEditarServicio(it)) },
                { nav.navigate(Routes.adminNuevoServicio) })
        }
    }
    composable(Routes.adminNuevoServicio) {
        StaffDest(sesion(), nav, administrador = true) {
            AdminServicioFormScreen(admin, catalogo, null) { nav.reiniciarListado(Routes.adminServicios) }
        }
    }
    composable(Routes.adminEditarServicio, arguments = listOf(idArgument("id"))) { entrada ->
        StaffDest(sesion(), nav, administrador = true) {
            AdminServicioFormScreen(admin, catalogo, entrada.id()) { nav.reiniciarListado(Routes.adminServicios) }
        }
    }
    composable(Routes.adminOperadores) {
        StaffDest(sesion(), nav, administrador = true) {
            AdminOperadoresScreen(admin, altaOperadoresDisponible()) { nav.navigate(Routes.adminNuevoOperador) }
        }
    }
    composable(Routes.adminNuevoOperador) {
        StaffDest(sesion(), nav, administrador = true) {
            if (altaOperadoresDisponible()) {
                NuevoOperadorScreen(admin) { nav.reiniciarListado(Routes.adminOperadores) }
            } else {
                Page("Nuevo operador") { Text("El servidor todavía no admite el alta por correo.") }
            }
        }
    }
}

@Composable
private fun StaffDest(sesion: Sesion?, nav: NavController, administrador: Boolean = false, contenido: @Composable () -> Unit) {
    val autorizado = if (administrador) sesion?.tiene("ADMINISTRADOR") == true
        else sesion?.tiene("ADMINISTRADOR", "OPERADOR") == true
    if (autorizado) contenido()
    else Page("Acceso restringido") {
        Primary("Ir a mi cuenta") { nav.reiniciar(Routes.portal(sesion)) }
    }
}

private fun NavController.reiniciarListado(ruta: String) {
    navigate(ruta) {
        popUpTo(ruta) { inclusive = true }
        launchSingleTop = true
    }
}
