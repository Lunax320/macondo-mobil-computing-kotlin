package com.example.macondo.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.macondo.data.Sesion
import com.example.macondo.data.repository.*
import com.example.macondo.ui.components.Page
import com.example.macondo.ui.components.Primary
import com.example.macondo.ui.screens.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MacondoApp(
    catalogo: CatalogRepository,
    cuentas: AccountRepository,
    reservas: ReservationRepository,
    operaciones: OperationsRepository,
    admin: AdminRepository
) {
    val sesion by cuentas.sesion.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var restaurando by remember { mutableStateOf(true) }
    var abrirPortalRestaurado by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            cuentas.restaurar()
            abrirPortalRestaurado = cuentas.sesion.value != null
        } finally {
            restaurando = false
        }
    }

    if (restaurando) {
        Page("Hotel Macondo") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        return
    }

    val actual = entrada?.destination?.route
    val clienteId = sesion?.cliente?.id
    val puedeVolver = nav.previousBackStackEntry != null
    val abrirPerfil = {
        nav.navigate(Routes.portal(sesion)) { launchSingleTop = true }
    }

    LaunchedEffect(sesion?.id, actual) {
        val session = sesion
        val ruta = actual ?: return@LaunchedEffect
        if (abrirPortalRestaurado) {
            abrirPortalRestaurado = false
            if (session != null && ruta == Routes.inicio) {
                nav.reiniciar(Routes.portal(session))
                return@LaunchedEffect
            }
        }
        if (session == null && Routes.protegida(ruta)) {
            nav.reiniciar(Routes.login())
        } else if (session != null && ruta == Routes.login) {
            val tipoId = entrada?.arguments?.getLong("tipoId")?.takeIf { it > 0 }
            val id = session.cliente?.id
            if (id != null) {
                nav.reiniciar(Routes.cliente(id))
                if (tipoId != null) nav.navigate(Routes.nuevaReserva(id, tipoId))
            } else {
                nav.reiniciar(Routes.portal(session))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Macondo") },
                navigationIcon = {
                    if (puedeVolver) TextButton(onClick = { nav.popBackStack() }) { Text("Atrás") }
                },
                actions = {
                    if (sesion != null) {
                        TextButton(onClick = { scope.launch { cuentas.salir(); nav.reiniciar(Routes.inicio) } }) {
                            Text("Salir")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (actual != null && ((!Routes.protegida(actual) && actual != Routes.login && actual != Routes.registro) ||
                    actual in setOf(Routes.cliente, Routes.operador, Routes.admin))) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { nav.inicio() }) { Text("Inicio") }
                        TextButton(onClick = { nav.navigate(Routes.habitaciones) { launchSingleTop = true } }) { Text("Habitaciones") }
                        TextButton(onClick = { nav.navigate(Routes.servicios) { launchSingleTop = true } }) { Text("Servicios") }
                        TextButton(onClick = abrirPerfil) { Text(if (sesion == null) "Entrar" else "Cuenta") }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = Routes.inicio, modifier = Modifier.padding(padding)) {
            composable(Routes.inicio) {
                InicioScreen(catalogo,
                    { nav.navigate(Routes.habitaciones) }, { nav.navigate(Routes.habitacion(it)) },
                    { nav.navigate(Routes.servicios) }, { nav.navigate(Routes.servicio(it)) },
                    { nav.navigate(Routes.explorar) })
            }
            composable(Routes.login, arguments = listOf(idArgument("tipoId", opcional = true))) {
                LoginScreen(cuentas) { nav.navigate(Routes.registro) }
            }
            composable(Routes.registro) {
                RegistroScreen(cuentas) {
                    scope.launch { snackbar.showSnackbar("Cuenta creada. Ya puedes iniciar sesión.") }
                    if (!nav.popBackStack()) nav.navigate(Routes.login())
                }
            }
            composable(Routes.habitaciones) {
                HabitacionesScreen(catalogo) { nav.navigate(Routes.habitacion(it)) }
            }
            composable(Routes.habitacion, arguments = listOf(idArgument("id"))) { destino ->
                DetalleHabitacionScreen(catalogo, destino.id()) { tipo ->
                    nav.navigate(if (clienteId == null) Routes.login(tipo) else Routes.nuevaReserva(clienteId, tipo))
                }
            }
            composable(Routes.servicios) { ServiciosScreen(catalogo) { nav.navigate(Routes.servicio(it)) } }
            composable(Routes.servicio, arguments = listOf(idArgument("id"))) { destino ->
                DetalleServicioScreen(catalogo, destino.id()) { nav.navigate(Routes.servicio(it)) }
            }
            composable(Routes.explorar) { ExplorarScreen() }

            staffRoutes(nav, { cuentas.sesion.value }, operaciones, admin, catalogo)

            composable(Routes.cliente, arguments = listOf(idArgument("id"))) { destino ->
                ClienteDest(destino, sesion, abrirPerfil) { id ->
                    val cliente = requireNotNull(sesion?.cliente)
                    PortalClienteScreen(cliente, { nav.navigate(Routes.activas(id)) },
                        { nav.navigate(Routes.nuevaReserva(id)) }, { nav.navigate(Routes.historial(id)) },
                        { nav.navigate(Routes.pagos(id)) }, { nav.navigate(Routes.perfil(id)) })
                }
            }
            composable(Routes.activas, arguments = listOf(idArgument("id"))) { destino ->
                ClienteDest(destino, sesion, abrirPerfil) { id ->
                    ReservasActivasScreen(reservas, id, { nav.navigate(Routes.editarReserva(id, it)) },
                        { nav.navigate(Routes.nuevaReserva(id)) })
                }
            }
            composable(Routes.historial, arguments = listOf(idArgument("id"))) { destino ->
                ClienteDest(destino, sesion, abrirPerfil) { HistorialScreen(reservas, cuentas, it) }
            }
            composable(Routes.pagos, arguments = listOf(idArgument("id"))) { destino ->
                ClienteDest(destino, sesion, abrirPerfil) { PagosScreen(reservas, it) }
            }
            composable(Routes.perfil, arguments = listOf(idArgument("id"))) { destino ->
                ClienteDest(destino, sesion, abrirPerfil) { PerfilScreen(cuentas, it) }
            }
            composable(Routes.nuevaReserva, arguments = listOf(idArgument("id"), idArgument("tipoId", opcional = true))) { destino ->
                ClienteDest(destino, sesion, abrirPerfil) { id ->
                    val tipoId = destino.id("tipoId").takeIf { it > 0 }
                    ReservaFormScreen(reservas, id, null, tipoId) { reserva ->
                        scope.launch { snackbar.showSnackbar("Reserva ${reserva.numeroReserva} confirmada.") }
                        nav.reiniciar(Routes.cliente(id))
                        nav.navigate(Routes.activas(id))
                    }
                }
            }
            composable(Routes.editarReserva, arguments = listOf(idArgument("id"), idArgument("reservaId"))) { destino ->
                ClienteDest(destino, sesion, abrirPerfil) { id ->
                    ReservaFormScreen(reservas, id, destino.id("reservaId"), null) {
                        scope.launch { snackbar.showSnackbar("Cambios guardados.") }
                        nav.reiniciar(Routes.cliente(id))
                        nav.navigate(Routes.activas(id))
                    }
                }
            }
        }
    }
}

@Composable
private fun ClienteDest(entrada: NavBackStackEntry, sesion: Sesion?, volver: () -> Unit, contenido: @Composable (Long) -> Unit) {
    val clienteId = sesion?.cliente?.id
    if (clienteId != null && entrada.id() == clienteId && sesion.tiene("CLIENTE")) contenido(clienteId)
    else Page("Acceso restringido") { Primary("Ir a mi cuenta", onClick = volver) }
}

internal fun idArgument(nombre: String, opcional: Boolean = false) = navArgument(nombre) {
    type = NavType.LongType
    if (opcional) defaultValue = -1L
}

internal fun NavBackStackEntry.id(nombre: String = "id") = requireNotNull(arguments).getLong(nombre)

internal fun NavController.reiniciar(ruta: String) {
    navigate(ruta) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavController.inicio() {
    navigate(Routes.inicio) {
        popUpTo(graph.startDestinationId)
        launchSingleTop = true
    }
}
