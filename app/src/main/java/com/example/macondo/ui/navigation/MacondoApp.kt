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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.macondo.R
import com.example.macondo.data.AccionChat
import com.example.macondo.data.Sesion
import com.example.macondo.data.remote.CapacidadesApi
import com.example.macondo.data.remote.ChatApi
import com.example.macondo.data.remote.DescubrimientoApi
import com.example.macondo.data.repository.*
import com.example.macondo.ui.chat.ChatViewModel
import com.example.macondo.ui.components.HotelImage
import com.example.macondo.ui.components.Page
import com.example.macondo.ui.components.Primary
import com.example.macondo.ui.screens.*
import com.example.macondo.ui.theme.Apariencia
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
    promociones: PromocionesRepository,
    chat: ChatApi,
    apariencia: Apariencia,
    cambiarApariencia: (Apariencia) -> Unit
) {
    val sesion by cuentas.sesion.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val chatModel: ChatViewModel = viewModel(factory = viewModelFactory { initializer { ChatViewModel(chat) } })
    val temaActual by rememberUpdatedState(apariencia)
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
    val pestanaActual = Pestana.paraRuta(actual)
    val tecladoVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val abrirPerfil = {
        nav.navigate(Routes.portal(sesion)) { launchSingleTop = true }
    }
    val cerrarSesion: () -> Unit = {
        scope.launch {
            cuentas.salir()
            chatModel.limpiar()
            nav.reiniciar(Routes.inicio)
            snackbar.showSnackbar("Sesión cerrada.")
        }
    }
    LaunchedEffect(sesion?.id) { chatModel.cambiarUsuario(sesion?.id) }

    LaunchedEffect(sesion?.id, actual) {
        val session = sesion
        val ruta = actual ?: return@LaunchedEffect
        if (session == null && Routes.protegida(ruta)) {
            nav.reiniciar(Routes.login())
        } else if (session != null && ruta == Routes.login) {
            val tipoId = entrada?.arguments?.getLong("tipoId")?.takeIf { it > 0 }
            val accion = AccionChat.desdeId(entrada?.arguments?.getString("accion"))
            val destinoChat = accion?.destino(session)?.ruta
            nav.reiniciar(Routes.portal(session))
            val id = session.cliente?.id
            when {
                destinoChat != null -> nav.navigate(destinoChat) { launchSingleTop = true }
                id != null && tipoId != null && session.tiene("CLIENTE") ->
                    nav.navigate(Routes.nuevaReserva(id, tipoId))
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    if (actual == Routes.configuracion) {
                        Text("Apariencia", style = MaterialTheme.typography.titleLarge)
                    } else if (actual == Routes.explorar) {
                        Text("Recorrido 3D", style = MaterialTheme.typography.titleLarge)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HotelImage("/images/logo.png", null, Modifier.size(26.dp))
                            Text("Macondo", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                },
                navigationIcon = {
                    if (actual == Routes.configuracion || actual == Routes.explorar) {
                        IconButton(onClick = {
                            if (!nav.popBackStack()) nav.reiniciar(Routes.inicio)
                        }) {
                            Icon(painterResource(R.drawable.ic_arrow_back), "Volver")
                        }
                    }
                },
                actions = {
                    if (actual != null && actual != Routes.configuracion) {
                        IconButton(onClick = {
                            nav.navigate(Routes.configuracion) { launchSingleTop = true }
                        }) {
                            Icon(painterResource(R.drawable.ic_settings), "Configurar apariencia")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (pestanaActual != null && !tecladoVisible) {
                MacondoNavigationBar(pestanaActual, if (sesion == null) "Iniciar sesión" else "Mi cuenta") { tab ->
                    if (tab == Pestana.CUENTA) {
                        if (actual != Routes.login || sesion != null) nav.abrirPrincipal(Routes.portal(sesion))
                    } else {
                        nav.abrirPrincipal(tab.ruta)
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
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
            composable(
                Routes.login,
                arguments = listOf(idArgument("tipoId", opcional = true), navArgument("accion") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) {
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
            composable(Routes.chat) {
                ChatScreen(chatModel, sesion) { ruta ->
                    nav.navigate(ruta) { launchSingleTop = true }
                }
            }
            composable(Routes.configuracion) {
                ConfiguracionScreen(temaActual, cambiarApariencia)
            }

            staffRoutes(
                nav, { sesion }, operaciones, admin, catalogo, promociones,
                promocionesDisponibles = { capacidades.administrarPromociones },
                altaOperadoresDisponible = { capacidades.altaOperadoresPorCorreo },
                cerrarSesion = cerrarSesion
            )

            composable(Routes.cliente, arguments = listOf(idArgument("id"))) { destino ->
                ClienteDest(destino, sesion, abrirPerfil) { id ->
                    val cliente = requireNotNull(sesion?.cliente)
                    PortalClienteScreen(cliente, { nav.navigate(Routes.activas(id)) },
                        { nav.navigate(Routes.nuevaReserva(id)) }, { nav.navigate(Routes.historial(id)) },
                        { nav.navigate(Routes.pagos(id)) }, { nav.navigate(Routes.perfil(id)) }, cerrarSesion)
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
                        proveedorPago = capacidades.proveedorPago
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

private fun NavController.abrirPrincipal(ruta: String) {
    if (currentDestination?.route == ruta) return
    if (currentDestination?.route != Routes.inicio &&
        !popBackStack(Routes.inicio, inclusive = false)
    ) reiniciar(Routes.inicio)
    if (ruta != Routes.inicio) navigate(ruta) { launchSingleTop = true }
}
