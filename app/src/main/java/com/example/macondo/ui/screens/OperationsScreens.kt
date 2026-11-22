package com.example.macondo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import com.example.macondo.data.*
import com.example.macondo.data.repository.OperationsRepository
import com.example.macondo.ui.components.*
import com.example.macondo.ui.screenModel

@Composable
fun OperadorScreen(reservas: () -> Unit, cuenta: () -> Unit, checkout: () -> Unit) {
    Page("Operación del hotel") {
        Primary("Gestionar reservas", onClick = reservas)
        Secondary("Buscar cuenta por habitación", onClick = cuenta)
        Secondary("Hacer checkout", onClick = checkout)
    }
}

@Composable
fun OperadorReservasScreen(operaciones: OperationsRepository, abrir: (String) -> Unit) {
    val model = screenModel("operador-reservas", operaciones::reservas)
    var filtro by rememberSaveable { mutableStateOf("TODAS") }
    var consulta by rememberSaveable { mutableStateOf("") }
    RemotePage("Reservas del hotel", model) { reservas, _ ->
        Selector("Estado", filtro, listOf("TODAS", "CONFIRMADA", "ACTIVA", "FINALIZADA", "CANCELADA"), { it }) { filtro = it }
        Field("Buscar por código, habitación o huésped", consulta) { consulta = it }
        val visibles = reservas.filter { reserva ->
            val texto = listOf(reserva.numeroReserva, reserva.habitacion?.numero, reserva.cliente?.nombreCompleto)
            (filtro == "TODAS" || reserva.estado == filtro) && texto.any { it.orEmpty().contains(consulta.trim(), true) }
        }
        if (visibles.isEmpty()) Text("No hay reservas para este filtro.")
        visibles.forEach { reserva ->
            ReservaCard(reserva) {
                reserva.cliente?.let { Text(it.nombreCompleto) }
                TextButton(onClick = { abrir(reserva.numeroReserva) }) { Text("Ver reserva") }
            }
        }
    }
}

@Composable
fun OperadorDetalleScreen(
    operaciones: OperationsRepository, numero: String, cuenta: (Long) -> Unit, checkout: (Long) -> Unit
) {
    val model = screenModel("operador-detalle:$numero") { operaciones.detalle(numero) }
    var accion by rememberSaveable { mutableStateOf<String?>(null) }
    RemotePage("Detalle de reserva", model) { reserva, estado ->
        ReservaCard(reserva)
        reserva.cliente?.let { cliente ->
            Panel {
                Section("Huésped")
                Text(cliente.nombreCompleto)
                cliente.correo?.let { Text(it) }
                cliente.telefono?.let { Text(it) }
            }
        }
        reserva.cuenta?.let { cuentaDetalle ->
            Panel {
                Section("Cuenta")
                Text("${cuentaDetalle.estado} · ${moneda(cuentaDetalle.total)}")
            }
        }
        if (reserva.estado == "CONFIRMADA") {
            Primary("Activar reserva", !estado.guardando) { accion = "Activar" }
            Secondary("Cancelar reserva", !estado.guardando) { accion = "Cancelar" }
        }
        if (reserva.estado == "ACTIVA") {
            Secondary("Ver cuenta") { cuenta(reserva.id) }
            Primary("Ir a checkout") { checkout(reserva.id) }
        }
    }
    accion?.let { elegida ->
        Confirmar("$elegida reserva", "Reserva $numero", elegida, cerrar = { accion = null }) {
            model.ejecutar(if (elegida == "Activar") "Reserva activada" else "Reserva cancelada") {
                if (elegida == "Activar") operaciones.activar(numero) else operaciones.cancelar(numero)
            }
        }
    }
}

@Composable
fun BuscarCuentaScreen(
    operaciones: OperationsRepository, checkout: Boolean, abrir: (Long) -> Unit
) {
    val model = screenModel("buscar-cuenta:$checkout") { Unit }
    var numero by rememberSaveable { mutableStateOf("") }
    RemotePage(if (checkout) "Checkout" else "Buscar cuenta", model) { _, estado ->
        Field("Número de habitación", numero) { numero = it }
        Primary("Buscar", numero.isNotBlank() && !estado.guardando) {
            val habitacion = numero.trim()
            model.ejecutar("Reserva encontrada", recargar = false) {
                val reserva = if (checkout) operaciones.checkout(habitacion) else operaciones.cuentaActual(habitacion)
                abrir(reserva.idReserva)
            }
        }
    }
}

@Composable
fun CuentaScreen(
    operaciones: OperationsRepository,
    reservaId: Long,
    checkout: Boolean,
    cerrar: () -> Unit
) {
    val model = screenModel("cuenta:$reservaId:$checkout") { operaciones.cuenta(reservaId) }
    var servicioId by rememberSaveable { mutableStateOf<Long?>(null) }
    var cantidad by rememberSaveable { mutableStateOf("1") }
    var medio by rememberSaveable { mutableStateOf("EFECTIVO") }
    var eliminar by remember { mutableStateOf<Consumo?>(null) }
    var confirmarPago by rememberSaveable { mutableStateOf(false) }
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    RemotePage(if (checkout) "Checkout" else "Cuenta de habitación", model) { datos, estado ->
        val reserva = datos.reserva
        val cuenta = reserva.cuenta
        val operable = !estado.guardando && !estado.cargando
        ReservaCard(reserva)
        if (cuenta == null) {
            Text("La reserva no tiene una cuenta asociada.")
        } else {
            Panel {
                Section("Cuenta ${cuenta.estado.lowercase()}")
                Text("Total · ${moneda(cuenta.total)}")
                val pagos = cuenta.pagos.orEmpty()
                val saldo = cuenta.total.subtract(pagos.filter { it.estado == "CONFIRMADO" }
                    .fold(java.math.BigDecimal.ZERO) { total, pago -> total.add(pago.monto) })
                Text("Saldo · ${moneda(saldo)}")
                pagos.forEach { Text("${it.metodoPago} · ${moneda(it.monto)} · ${it.estado}") }
            }

            Section("Consumos")
            val abierta = reserva.estado == "ACTIVA" && cuenta.estado == "ABIERTA"
            val consumos = cuenta.consumos.orEmpty()
            if (consumos.isEmpty()) Text("No hay consumos registrados.")
            consumos.forEach { consumo ->
                Panel {
                    Text("${consumo.cantidad} × ${consumo.servicio}")
                    Text(moneda(consumo.precio))
                    if (abierta) TextButton(enabled = operable, onClick = { eliminar = consumo }) {
                        Text("Quitar consumo")
                    }
                }
            }
            if (abierta && !checkout) {
                val servicio = datos.servicios.firstOrNull { it.id == servicioId }
                Selector("Servicio", servicio, datos.servicios, { it.nombre }) { servicioId = it.id }
                Field("Cantidad", cantidad, tipo = KeyboardType.Number) { cantidad = it.filter(Char::isDigit) }
                Primary("Agregar consumo", servicioId != null && (cantidad.toIntOrNull() ?: 0) > 0 && operable) {
                    val id = servicioId ?: return@Primary
                    val unidades = cantidad.toIntOrNull() ?: return@Primary
                    model.ejecutar("Consumo agregado") {
                        operaciones.agregarConsumos(reserva.numeroReserva, listOf(ConsumoSolicitud(id, unidades)))
                    }
                }
            }
            if (checkout && reserva.estado == "ACTIVA") {
                if (abierta) {
                    Selector("Método de pago", medio, listOf("EFECTIVO", "TARJETA", "TRANSFERENCIA"), { it }) { medio = it }
                    Primary("Registrar pago", operable) { confirmarPago = true }
                }
                Primary("Finalizar estadía", operable && cuenta.estado == "PAGADA") { confirmarSalida = true }
            }
            eliminar?.let { consumo ->
                Confirmar("Quitar consumo", consumo.servicio, "Quitar", cerrar = { eliminar = null }) {
                    model.ejecutar("Consumo eliminado") { operaciones.eliminarConsumo(reserva.numeroReserva, consumo.id) }
                }
            }
            if (confirmarPago) {
                Confirmar("Registrar pago", "Se registrará el saldo de la cuenta por $medio.", "Confirmar pago",
                    cerrar = { confirmarPago = false }) {
                    val metodo = medio
                    model.ejecutar("Pago registrado") { operaciones.pagar(reserva.numeroReserva, PagoSolicitud(metodo)) }
                }
            }
            if (confirmarSalida) {
                Confirmar("Finalizar estadía", "Se cerrará la reserva ${reserva.numeroReserva}.", "Finalizar",
                    cerrar = { confirmarSalida = false }) {
                    model.ejecutar("Estadía finalizada", recargar = false) {
                        operaciones.finalizar(reserva.numeroReserva)
                        cerrar()
                    }
                }
            }
        }
    }
}
