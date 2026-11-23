package com.example.macondo.ui.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.macondo.data.Sesion
import com.example.macondo.data.remote.CapacidadesApi
import com.example.macondo.data.remote.DescubrimientoApi
import com.example.macondo.data.repository.*
import com.example.macondo.ui.components.HotelImage
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
    admin: AdminRepository,
    descubrimiento: DescubrimientoApi,
    promociones: PromocionesRepository
) {
    val sesion by cuentas.sesion.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var restaurando by remember { mutableStateOf(true) }
    var capacidades by remember { mutableStateOf(CapacidadesApi()) }

    LaunchedEffect(descubrimiento) { capacidades = descubrimiento.consultar() }

    LaunchedEffect(Unit) {
        try {
            cuentas.restaurar()
        } finally {
            restaurando = false
        }
    }

    if (restaurando) {
        Page("Hotel Macondo") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        return
    }

    val actual = entrada?.destination?.route
    val puedeVolver = nav.previousBackStackEntry != null
    val abrirPerfil = {
        nav.navigate(Routes.portal(sesion)) { launchSingleTop = true }
    }

    LaunchedEffect(sesion?.id, actual) {
        val session = sesion
        val ruta = actual ?: return@LaunchedEffect
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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HotelImage("/images/logo.png", null, Modifier.size(26.dp))
                        Text("Macondo", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        },
        bottomBar = {
            if (actual != null && ((!Routes.protegida(actual) && actual !in setOf(Routes.login, Routes.registro, Routes.recuperarContrasena)) ||
                    actual in setOf(Routes.cliente, Routes.operador, Routes.admin))) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding()
                    ) {
                        NavItem("Inicio") { nav.inicio() }
                        NavItem("Habitaciones") { nav.navigate(Routes.habitaciones) { launchSingleTop = true } }
                        NavItem("Servicios") { nav.navigate(Routes.servicios) { launchSingleTop = true } }
                        NavItem(if (sesion == null) "Entrar" else "Cuenta", abrirPerfil)
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(
                nav,
                startDestination = Routes.inicio,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
            ) {
            composable(Routes.inicio) {
                InicioScreen(catalogo,
                    { nav.navigate(Routes.habitaciones) }, { nav.navigate(Routes.habitacion(it)) },
                    { nav.navigate(Routes.servicios) }, { nav.navigate(Routes.servicio(it)) },
                    { nav.navigate(Routes.explorar) },
                    promociones.takeIf { capacidades.promocionesPublicas },
                    { nav.navigate(Routes.promociones) })
            }
            composable(Routes.promociones) {
                if (capacidades.promocionesPublicas) PromocionesScreen(promociones)
                else Page("Promociones") { Text("Las promociones no están disponibles en este servidor.") }
            }
            composable(Routes.login, arguments = listOf(idArgument("tipoId", opcional = true))) {
                LoginScreen(cuentas, capacidades.verificacionCorreo,
                    recuperar = if (capacidades.recuperarContrasena) ({ nav.navigate(Routes.recuperarContrasena) }) else null
                ) { nav.navigate(Routes.registro) }
            }
            composable(Routes.registro) {
                RegistroScreen(cuentas, capacidades.verificacionCorreo) { mensaje ->
                    scope.launch { snackbar.showSnackbar(mensaje) }
                    if (!nav.popBackStack()) nav.navigate(Routes.login())
                }
            }
            composable(Routes.recuperarContrasena) {
                if (capacidades.recuperarContrasena) RecuperarContrasenaScreen(cuentas)
                else Page("Recuperar contraseña") { Text("La recuperación no está disponible en este servidor.") }
            }
            composable(Routes.habitaciones) {
                HabitacionesScreen(catalogo) { nav.navigate(Routes.habitacion(it)) }
            }
            composable(Routes.habitacion, arguments = listOf(idArgument("id"))) { destino ->
                DetalleHabitacionScreen(catalogo, destino.id()) { tipo ->
                    val clienteId = sesion?.cliente?.id
                    nav.navigate(if (clienteId == null) Routes.login(tipo) else Routes.nuevaReserva(clienteId, tipo))
                }
            }
            composable(Routes.servicios) { ServiciosScreen(catalogo) { nav.navigate(Routes.servicio(it)) } }
            composable(Routes.servicio, arguments = listOf(idArgument("id"))) { destino ->
                DetalleServicioScreen(catalogo, destino.id()) { nav.navigate(Routes.servicio(it)) }
            }
            composable(Routes.explorar) { ExplorarScreen() }

            staffRoutes(nav, { sesion }, operaciones, admin, catalogo, promociones) { capacidades.administrarPromociones }

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
                        { nav.navigate(Routes.nuevaReserva(id)) },
                        { nav.navigate(Routes.detalleReserva(id, it)) })
                }
            }
            composable(
                Routes.detalleReserva,
                arguments = listOf(idArgument("id"), idArgument("reservaId"), navArgument("historial") {
                    type = NavType.BoolType
                    defaultValue = false
                })
            ) { destino ->
                ClienteDest(destino, sesion, abrirPerfil) { id ->
                    ReservaDetalleScreen(
                        reservas, id, destino.id("reservaId"),
                        historial = destino.arguments?.getBoolean("historial") == true,
                        cuentaDisponible = capacidades.detalleReserva,
                        pagoDisponible = capacidades.detalleReserva && capacidades.wompi
                    )
                }
            }
            composable(Routes.historial, arguments = listOf(idArgument("id"))) { destino ->
                ClienteDest(destino, sesion, abrirPerfil) { id ->
                    HistorialScreen(reservas, cuentas, id) { nav.navigate(Routes.detalleReserva(id, it, historial = true)) }
                }
            }
            composable(Routes.pagos, arguments = listOf(idArgument("id"))) { destino ->
                ClienteDest(destino, sesion, abrirPerfil) { PagosScreen(reservas, it) }
            }
            composable(Routes.perfil, arguments = listOf(idArgument("id"))) { destino ->
                ClienteDest(destino, sesion, abrirPerfil) { id ->
                    PerfilScreen(cuentas, id, capacidades.verificacionCorreo, alCerrarSesion = {
                        nav.reiniciar(Routes.inicio)
                    }) {
                        nav.navigate(Routes.cambiarContrasena(id)) { launchSingleTop = true }
                    }
                }
            }
            composable(Routes.cambiarContrasena, arguments = listOf(idArgument("id"))) { destino ->
                ClienteDest(destino, sesion, abrirPerfil) { id ->
                    CambiarContrasenaScreen(cuentas, id,
                        cancelar = { nav.popBackStack(Routes.perfil(id), inclusive = false) },
                        alGuardar = {
                            nav.popBackStack(Routes.perfil(id), inclusive = false)
                            scope.launch { snackbar.showSnackbar("Contraseña actualizada.") }
                        })
                }
            }
            composable(Routes.nuevaReserva, arguments = listOf(idArgument("id"), idArgument("tipoId", opcional = true))) { destino ->
                ClienteDest(destino, sesion, abrirPerfil) { id ->
                    val tipoId = destino.id("tipoId").takeIf { it > 0 }
                    ReservaFormScreen(reservas, id, null, tipoId, promociones.takeIf { capacidades.calcularPromocion }) { reserva ->
                        scope.launch { snackbar.showSnackbar("Reserva ${reserva.numeroReserva} confirmada.") }
                        nav.reiniciar(Routes.cliente(id))
                        nav.navigate(Routes.activas(id))
                    }
                }
            }
            composable(Routes.editarReserva, arguments = listOf(idArgument("id"), idArgument("reservaId"))) { destino ->
                ClienteDest(destino, sesion, abrirPerfil) { id ->
                    ReservaFormScreen(reservas, id, destino.id("reservaId"), null,
                        promociones.takeIf { capacidades.calcularPromocion }) {
                        scope.launch { snackbar.showSnackbar("Cambios guardados.") }
                        nav.reiniciar(Routes.cliente(id))
                        nav.navigate(Routes.activas(id))
                    }
                }
            }
            }
            if (puedeVolver) {
                Box(
                    Modifier.align(Alignment.CenterStart).width(24.dp).fillMaxHeight()
                        .backSwipeEdge(nav, desdeDerecha = false)
                )
                Box(
                    Modifier.align(Alignment.CenterEnd).width(24.dp).fillMaxHeight()
                        .backSwipeEdge(nav, desdeDerecha = true)
                )
            }
        }
    }
}

private fun Modifier.backSwipeEdge(nav: NavController, desdeDerecha: Boolean) = pointerInput(nav, desdeDerecha) {
    val recorridoMinimo = 96.dp.toPx()
    var desplazamiento = 0f
    detectHorizontalDragGestures(
        onHorizontalDrag = { cambio, delta ->
            desplazamiento += delta
            cambio.consume()
        },
        onDragEnd = {
            val completo = if (desdeDerecha) desplazamiento <= -recorridoMinimo
                else desplazamiento >= recorridoMinimo
            desplazamiento = 0f
            if (completo) nav.popBackStack()
        },
        onDragCancel = { desplazamiento = 0f }
    )
}

@Composable
private fun RowScope.NavItem(label: String, abrir: () -> Unit) {
    TextButton(
        onClick = abrir,
        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
    ) {
        Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
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
