package com.example.macondo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.macondo.data.*
import com.example.macondo.data.remote.mensajeParaUsuario
import com.example.macondo.data.repository.AccountRepository
import com.example.macondo.data.repository.PromocionesRepository
import com.example.macondo.data.repository.ReservationRepository
import com.example.macondo.ui.components.*
import com.example.macondo.ui.screenModel
import java.time.LocalDate
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Composable
fun PortalClienteScreen(
    cliente: Cliente, abrirReservas: () -> Unit, nueva: () -> Unit,
    historial: () -> Unit, pagos: () -> Unit, perfil: () -> Unit
) {
    Page("Hola, ${cliente.nombre}") {
        Primary("Nueva reserva", onClick = nueva)
        Secondary("Mis reservas", onClick = abrirReservas)
        Secondary("Historial de estadías", onClick = historial)
        Secondary("Historial de pagos", onClick = pagos)
        Secondary("Mi perfil", onClick = perfil)
    }
}

@Composable
fun ReservasActivasScreen(
    reservas: ReservationRepository, clienteId: Long, editar: (Long) -> Unit, nueva: () -> Unit,
    abrirDetalle: (Long) -> Unit
) {
    val model = screenModel("activas:$clienteId") { reservas.activas(clienteId) }
    var seleccion by remember { mutableStateOf<Reserva?>(null) }
    RemotePage("Mis reservas", model) { activas, estado ->
        Primary("Nueva reserva", onClick = nueva)
        if (activas.isEmpty()) Text("Todavía no tienes reservas activas.")
        activas.forEach { reserva ->
            ReservaCard(reserva, abrirDetalle = { abrirDetalle(reserva.id) }) {
                if (reserva.fechaInicio > LocalDate.now().toString() &&
                    reserva.estado in setOf("CONFIRMADA", "ACTIVA")) {
                    Secondary("Editar reserva", !estado.guardando && !estado.cargando) { editar(reserva.id) }
                    TextButton(
                        enabled = !estado.guardando && !estado.cargando,
                        onClick = { seleccion = reserva }
                    ) { Text("Cancelar reserva") }
                }
            }
        }
    }
    seleccion?.let { reserva ->
        Confirmar("Cancelar reserva", "Se cancelará la reserva ${reserva.numeroReserva}.", "Cancelar reserva",
            cerrar = { seleccion = null }) {
            model.ejecutar("Reserva cancelada") { reservas.cancelar(clienteId, reserva.id) }
        }
    }
}

@Composable
fun ReservaCard(
    reserva: Reserva, abrirDetalle: (() -> Unit)? = null,
    acciones: @Composable ColumnScope.() -> Unit = {}
) {
    val modifier = if (abrirDetalle == null) Modifier
        else Modifier.clickable(onClickLabel = "Ver detalle de la reserva", onClick = abrirDetalle)
    Panel(modifier) {
        Section(reserva.numeroReserva)
        Text("${reserva.fechaInicio} al ${reserva.fechaFin} · ${reserva.cantidadPersonas} huéspedes")
        Text(reserva.estado)
        reserva.habitacion?.let { Text("${it.nombre} · habitación ${it.numero}") }
        Text(moneda(reserva.total), style = MaterialTheme.typography.titleMedium)
        acciones()
    }
}

private data class DisponibilidadConsulta(val tipoId: Long, val solicitud: ReservaSolicitud)

private data class Disponibilidad(
    val consulta: DisponibilidadConsulta? = null,
    val cargando: Boolean = false,
    val unidades: List<Habitacion>? = null,
    val error: String? = null
)

private class AvailabilityModel : ViewModel() {
    private val estadoMutable = MutableStateFlow(Disponibilidad())
    val estado = estadoMutable.asStateFlow()
    private var trabajo: Job? = null

    fun invalidar() {
        trabajo?.cancel()
        estadoMutable.value = Disponibilidad()
    }

    fun consultar(consulta: DisponibilidadConsulta, cargar: suspend () -> List<Habitacion>) {
        invalidar()
        estadoMutable.value = Disponibilidad(consulta, cargando = true)
        trabajo = viewModelScope.launch {
            try {
                estadoMutable.value = Disponibilidad(consulta, unidades = cargar())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                estadoMutable.value = Disponibilidad(consulta, error = error.mensajeParaUsuario())
            }
        }
    }
}

@Composable
fun ReservaFormScreen(
    reservas: ReservationRepository, clienteId: Long, reservaId: Long?, tipoInicial: Long?,
    promociones: PromocionesRepository? = null,
    alGuardar: (Reserva) -> Unit
) {
    val model = screenModel("reserva:$clienteId:$reservaId") { reservas.formulario(clienteId, reservaId) }
    val disponibilidadModel: AvailabilityModel = viewModel(factory = viewModelFactory { initializer { AvailabilityModel() } })
    val disponibilidad by disponibilidadModel.estado.collectAsStateWithLifecycle()
    val promoModel = screenModel("cotizar-promocion") { Unit }
    val estadoPromo by promoModel.estado.collectAsStateWithLifecycle()

    RemotePage(if (reservaId == null) "Nueva reserva" else "Editar reserva", model) { datos, estado ->
        val reserva = datos.reserva
        var codigo by rememberSaveable(reserva?.id) { mutableStateOf(reserva?.codigoPromocional.orEmpty()) }
        var solicitudPromo by remember(reserva?.id) { mutableStateOf<SolicitudPromocion?>(null) }
        var resultadoPromo by remember(reserva?.id) { mutableStateOf<PromocionAplicada?>(null) }
        var tipoId by rememberSaveable(reserva?.id, tipoInicial) {
            mutableStateOf(reserva?.habitacion?.tipoHabitacion?.id ?: tipoInicial)
        }
        var entrada by rememberSaveable(reserva?.id) { mutableStateOf(reserva?.fechaInicio.orEmpty()) }
        var salida by rememberSaveable(reserva?.id) { mutableStateOf(reserva?.fechaFin.orEmpty()) }
        var personas by rememberSaveable(reserva?.id) { mutableIntStateOf(reserva?.cantidadPersonas ?: 1) }
        val capacidadMaxima = (datos.tipos.maxOfOrNull { it.capacidadPersonas } ?: 1).coerceAtLeast(1)
        val tipo = datos.tipos.firstOrNull { it.id == tipoId }
        val cantidad = personas.coerceIn(1, capacidadMaxima)
        val fechaInicio = runCatching { LocalDate.parse(entrada) }.getOrNull()
        val fechaFin = runCatching { LocalDate.parse(salida) }.getOrNull()
        val consulta = tipoId?.let { seleccionado ->
            if (fechaInicio == null || fechaFin == null || tipo == null ||
                fechaInicio.isBefore(LocalDate.now()) || !fechaFin.isAfter(fechaInicio) ||
                cantidad !in 1..tipo.capacidadPersonas) null
            else DisponibilidadConsulta(seleccionado, ReservaSolicitud(entrada, salida, cantidad))
        }
        val resultado = disponibilidad.takeIf { consulta != null && it.consulta == consulta }
        val criteriosPromo = if (promociones != null && consulta != null && tipo != null && codigo.isNotBlank()) {
            SolicitudPromocion(codigo.trim(), consulta.tipoId, entrada, salida, tipo.precioNoche)
        } else null
        val promoVigente = resultadoPromo.takeIf { criteriosPromo != null && solicitudPromo == criteriosPromo }
        val codigoListo = reservaId != null || promociones == null || codigo.isBlank() || promoVigente?.codigo != null
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Section("Fechas de estadía")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField("Entrada", entrada, Modifier.weight(1f)) {
                    entrada = it
                    disponibilidadModel.invalidar()
                }
                DateField("Salida", salida, Modifier.weight(1f)) {
                    salida = it
                    disponibilidadModel.invalidar()
                }
            }
            if (fechaInicio != null && fechaFin != null && fechaFin.isAfter(fechaInicio)) {
                val noches = java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, fechaFin)
                Text("$noches ${if (noches == 1L) "noche" else "noches"}", style = MaterialTheme.typography.titleMedium)
            }
            if (fechaInicio != null && fechaInicio.isBefore(LocalDate.now())) {
                Text("La entrada debe ser hoy o después.", color = MaterialTheme.colorScheme.error)
            }
            if (fechaInicio != null && fechaFin != null && !fechaFin.isAfter(fechaInicio)) {
                Text("La salida debe ser posterior a la entrada.", color = MaterialTheme.colorScheme.error)
            }
        }
        if (capacidadMaxima > 1) {
            SliderField("Huéspedes", cantidad, 1..capacidadMaxima, "personas") {
                personas = it
                if (tipo != null && it > tipo.capacidadPersonas) tipoId = null
                disponibilidadModel.invalidar()
            }
        } else if (datos.tipos.isNotEmpty()) {
            Text("1 huésped")
        }
        val tiposDisponibles = datos.tipos.filter { it.id != null && it.capacidadPersonas >= cantidad }
        if (tiposDisponibles.isEmpty()) {
            Text("No hay opciones de habitación para esta cantidad de huéspedes.")
        } else {
            Selector("Tipo de habitación", tipo, tiposDisponibles, { it.nombre }) {
                tipoId = it.id
                disponibilidadModel.invalidar()
            }
            tipo?.let {
                Text("Hasta ${it.capacidadPersonas} huéspedes · ${moneda(it.precioNoche)} por noche")
            }
        }

        Primary("Consultar disponibilidad", consulta != null && resultado?.cargando != true && !estado.guardando) {
            val criterios = consulta ?: return@Primary
            disponibilidadModel.consultar(criterios) {
                reservas.buscarDisponibilidad(clienteId, criterios.tipoId, criterios.solicitud, reservaId)
            }
        }
        resultado?.let { disponibilidadActual ->
            FormSection("Disponibilidad") {
                if (disponibilidadActual.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
                disponibilidadActual.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                disponibilidadActual.unidades?.let { unidades ->
                    Text(if (unidades.isEmpty()) "No hay habitaciones para estas fechas. Prueba otros días."
                        else "Encontramos ${unidades.size} ${if (unidades.size == 1) "habitación" else "habitaciones"}.")
                }
            }
        }
        if (consulta != null && resultado?.unidades?.isNotEmpty() == true &&
            tipo != null && fechaInicio != null && fechaFin != null) {
            val noches = java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, fechaFin)
            val subtotal = tipo.precioNoche.multiply(noches.toBigDecimal())
            FormSection("Resumen de la estadía") {
                Text("${tipo.nombre} · $noches ${if (noches == 1L) "noche" else "noches"}")
                Text("Alojamiento estimado · ${moneda(subtotal)}")
                if (promociones != null && (reservaId == null || codigo.isNotBlank())) {
                    CodigoPromocionalSection(
                        codigo = codigo,
                        editable = reservaId == null,
                        habilitado = !estado.guardando && !estadoPromo.guardando,
                        aplicada = promoVigente,
                        descuentoGuardado = reserva?.descuentoPromocional,
                        error = estadoPromo.error.takeIf { solicitudPromo == criteriosPromo },
                        cambiarCodigo = { nuevo -> codigo = nuevo; solicitudPromo = null; resultadoPromo = null }
                    ) {
                        val criterios = criteriosPromo ?: return@CodigoPromocionalSection
                        solicitudPromo = criterios
                        resultadoPromo = null
                        promoModel.ejecutar("Promoción calculada", recargar = false) {
                            resultadoPromo = promociones.calcular(
                                criterios.codigo, criterios.entrada, criterios.salida, criterios.precioNoche
                            )
                        }
                    }
                }
            }
            Primary(if (reservaId == null) "Confirmar reserva" else "Guardar cambios",
                codigoListo && !estado.guardando && !estadoPromo.guardando) {
                val solicitud = if (reservaId == null) consulta.solicitud.copy(codigoPromocional = promoVigente?.codigo)
                    else consulta.solicitud
                model.ejecutar("Reserva guardada", recargar = false) {
                    alGuardar(reservas.guardar(clienteId, consulta.tipoId, solicitud, reservaId))
                }
            }
        }
    }
}

@Composable
private fun CodigoPromocionalSection(
    codigo: String, editable: Boolean, habilitado: Boolean, aplicada: PromocionAplicada?,
    descuentoGuardado: BigDecimal?, error: String?, cambiarCodigo: (String) -> Unit, calcular: () -> Unit
) {
    HorizontalDivider()
    if (editable) {
        Field("Código promocional (opcional)", codigo, isError = error != null, supportingText = error) { cambiarCodigo(it) }
    } else {
        Text("Código actual · $codigo")
        descuentoGuardado?.let { Text("Descuento anterior · ${moneda(it)}") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = calcular, enabled = habilitado && codigo.isNotBlank()) {
            Text(if (editable) "Aplicar código" else "Recalcular descuento")
        }
        if (editable && codigo.isNotBlank()) {
            TextButton(onClick = { cambiarCodigo("") }, enabled = habilitado) { Text("Quitar") }
        }
    }
    aplicada?.let {
        Text("Descuento · ${moneda(it.descuento)}")
        Text("Total estimado · ${moneda(it.total)}", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun HistorialScreen(
    reservas: ReservationRepository, cuentas: AccountRepository, clienteId: Long, abrirDetalle: (Long) -> Unit
) {
    val model = screenModel("historial:$clienteId") { reservas.historialConComentarios(clienteId) }
    var filtro by rememberSaveable { mutableStateOf("TODAS") }
    var editando by remember { mutableStateOf<Reserva?>(null) }
    var texto by rememberSaveable { mutableStateOf("") }
    var calificacion by rememberSaveable { mutableIntStateOf(5) }
    var borrar by remember { mutableStateOf<Comentario?>(null) }

    RemotePage("Historial de reservas", model) { datos, estado ->
        if (datos.comentarios == null) Text("Los comentarios no están disponibles en este servidor.")
        ChoiceChips("Filtrar por estado", filtro, listOf("TODAS", "FINALIZADA", "CANCELADA"), { it }) { filtro = it }
        val visibles = datos.reservas.filter { filtro == "TODAS" || it.estado == filtro }
        if (visibles.isEmpty()) Text("No hay reservas en el historial para este filtro.")
        visibles.forEach { reserva ->
            val comentario = datos.comentarios?.get(reserva.id)
            ReservaCard(reserva, abrirDetalle = { abrirDetalle(reserva.id) }) {
                if (datos.comentarios != null && reserva.estado == "FINALIZADA") {
                    comentario?.let {
                        Text(it.texto)
                        val estadoComentario = if (it.publicado) "Publicado" else "Pendiente · referencia ${it.id}"
                        Text("${it.calificacion} de 5 · $estadoComentario")
                    }
                    Secondary(if (comentario == null) "Comentar estadía" else "Editar comentario", !estado.guardando) {
                        editando = reserva
                        texto = comentario?.texto.orEmpty()
                        calificacion = comentario?.calificacion ?: 5
                    }
                    if (comentario != null) TextButton(onClick = { borrar = comentario }, enabled = !estado.guardando) {
                        Text("Eliminar comentario")
                    }
                }
            }
        }
        editando?.let { reserva ->
            val comentario = datos.comentarios?.get(reserva.id)
            AlertDialog(
                onDismissRequest = { editando = null },
                title = { Text(if (comentario == null) "Comentar estadía" else "Editar comentario") },
                text = {
                    Column {
                        ChoiceChips("Tu calificación", calificacion, (1..5).toList(), { "$it ★" }) { calificacion = it }
                        Field("Tu comentario", texto, multilinea = true) { if (it.length <= 500) texto = it }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val nota = calificacion
                        val contenido = texto.trim()
                        editando = null
                        model.ejecutar("Comentario enviado a revisión") {
                            if (comentario == null) cuentas.comentar(ComentarioSolicitud(clienteId, reserva.id, nota, contenido))
                            else cuentas.editarComentario(comentario.id, clienteId, ComentarioEdicion(nota, contenido))
                        }
                    }, enabled = texto.isNotBlank() && !estado.guardando) { Text("Guardar") }
                },
                dismissButton = { TextButton(onClick = { editando = null }) { Text("Cancelar") } }
            )
        }
    }
    borrar?.let { comentario ->
        Confirmar("Eliminar comentario", "Se quitará el comentario de la estadía.", "Eliminar",
            cerrar = { borrar = null }) {
            model.ejecutar("Comentario eliminado") { cuentas.borrarComentario(comentario.id, clienteId) }
        }
    }
}

@Composable
fun PagosScreen(reservas: ReservationRepository, clienteId: Long) {
    val model = screenModel("pagos:$clienteId") { reservas.pagos(clienteId) }
    RemotePage("Historial de pagos", model) { pagos, _ ->
        if (pagos.isEmpty()) Text("Todavía no tienes pagos registrados.")
        pagos.forEach { pago ->
            Panel {
                Section(pago.numeroReserva ?: "Pago registrado")
                Text(moneda(pago.monto))
                Text("${pago.metodoPago} · ${pago.estado}")
                Text(pago.fechaPago.replace('T', ' '))
            }
        }
    }
}

@Composable
fun PerfilScreen(
    cuentas: AccountRepository, clienteId: Long, verificacionCorreo: Boolean,
    abrirCambioContrasena: () -> Unit
) {
    val model = screenModel("perfil:$clienteId") { cuentas.perfil(clienteId) }
    var borrar by rememberSaveable { mutableStateOf(false) }

    RemotePage("Mi perfil", model) { perfil, estado ->
        ClienteFields(perfil, soloGmail = verificacionCorreo) { solicitud, valido ->
            if (!perfil.correo.equals(solicitud.correo, ignoreCase = true)) {
                Text(if (verificacionCorreo) "Verifica el correo nuevo antes de volver a iniciar sesión."
                    else "Si cambias el correo, deberás volver a iniciar sesión.")
            }
            Primary("Guardar perfil", valido && !estado.guardando) {
                model.ejecutar("Perfil actualizado") { cuentas.actualizarPerfil(clienteId, solicitud) }
            }
        }
        HorizontalDivider()
        Secondary("Cambiar contraseña", enabled = !estado.guardando, onClick = abrirCambioContrasena)
        TextButton(enabled = !estado.guardando, onClick = { borrar = true }) { Text("Eliminar mi cuenta") }
    }
    if (borrar) {
        Confirmar("Eliminar la cuenta", "Esta acción elimina tu cuenta de Hotel Macondo.", "Eliminar cuenta",
            cerrar = { borrar = false }) {
            model.ejecutar("Cuenta eliminada", recargar = false) {
                cuentas.borrarCuenta(clienteId)
                cuentas.salir()
            }
        }
    }
}
