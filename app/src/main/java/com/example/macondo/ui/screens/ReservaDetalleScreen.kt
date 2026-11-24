package com.example.macondo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.macondo.data.CuentaDetalleCliente
import com.example.macondo.data.ProveedorPago
import com.example.macondo.data.ReservaDetalleCliente
import com.example.macondo.data.repository.ReservationRepository
import com.example.macondo.ui.components.*
import com.example.macondo.ui.screenModel
import java.math.BigDecimal
import java.net.URI

@Composable
fun ReservaDetalleScreen(
    reservas: ReservationRepository, clienteId: Long, reservaId: Long,
    historial: Boolean, cuentaDisponible: Boolean, proveedorPago: ProveedorPago?
) {
    val model = screenModel("detalle:$clienteId:$reservaId:$historial:$cuentaDisponible") {
        reservas.detalle(clienteId, reservaId, historial, cuentaDisponible)
    }
    val navegador = LocalUriHandler.current
    var esperandoRegreso by rememberSaveable(reservaId) { mutableStateOf(false) }
    var checkoutAbierto by rememberSaveable(reservaId) { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (esperandoRegreso) {
            esperandoRegreso = false
            model.recargar()
        }
    }
    RemotePage("Detalle de reserva", model) { detalle, estado ->
        ResumenEstadia(detalle)
        DescuentoReserva(detalle.codigoPromocional, detalle.descuentoPromocional)
        detalle.cuenta?.let { cuenta ->
            SaldoCuenta(cuenta) {
                if (proveedorPago != null && detalle.estado.equals("ACTIVA", ignoreCase = true) && cuenta.saldo.signum() > 0) {
                    PagoEnLineaSection(
                        saldo = cuenta.saldo,
                        proveedor = proveedorPago,
                        habilitado = !estado.guardando && !estado.cargando && !esperandoRegreso
                    ) { monto ->
                        model.ejecutar("Consulta los pagos registrados al regresar.", recargar = false) {
                            val checkout = reservas.iniciarPago(proveedorPago, clienteId, reservaId, monto)
                            val url = URI.create(checkout.checkoutUrl)
                            require(url.esCheckoutSeguro(proveedorPago)) { "Dirección de checkout inválida." }
                            esperandoRegreso = true
                            try {
                                navegador.openUri(url.toString())
                                checkoutAbierto = true
                            } catch (error: Exception) {
                                esperandoRegreso = false
                                throw error
                            }
                        }
                    }
                }
                if (checkoutAbierto) {
                    Text("El saldo se actualiza cuando el hotel recibe la confirmación del pago.")
                    TextButton(onClick = {
                        esperandoRegreso = false
                        model.recargar()
                    }, enabled = !estado.guardando && !estado.cargando) { Text("Actualizar saldo") }
                }
            }
            CargosCuenta(detalle.total, cuenta)
            PagosRegistrados(cuenta)
        }
    }
}

@Composable
private fun PagoEnLineaSection(
    saldo: BigDecimal,
    proveedor: ProveedorPago,
    habilitado: Boolean,
    pagar: (BigDecimal) -> Unit
) {
    var parcial by rememberSaveable { mutableStateOf(false) }
    var valor by rememberSaveable(saldo.toPlainString()) { mutableStateOf(saldo.toPlainString()) }
    val monto = if (parcial) valor.trim().replace(',', '.').toBigDecimalOrNull() else saldo
    val valido = monto != null && monto.signum() > 0 && monto <= saldo && monto.stripTrailingZeros().scale() <= 2
    SwitchField("Pagar otro monto", parcial, { parcial = it })
    if (parcial) Field("Monto en COP", valor, tipo = KeyboardType.Decimal,
        isError = !valido, supportingText = if (valido) null else "Escribe un importe positivo, hasta el saldo.") { valor = it }
    Primary("Continuar a ${proveedor.nombre}", habilitado && valido) { monto?.let(pagar) }
}

private fun URI.esCheckoutSeguro(proveedor: ProveedorPago): Boolean {
    if (!scheme.equals("https", ignoreCase = true) || userInfo != null || port !in listOf(-1, 443)) {
        return false
    }
    val dominio = host?.lowercase() ?: return false
    return when (proveedor) {
        ProveedorPago.WOMPI -> dominio == "checkout.wompi.co"
        ProveedorPago.MERCADO_PAGO -> listOf("mercadopago.com.co", "mercadopago.com").any {
            dominio == it || dominio.endsWith(".$it")
        }
    }
}

@Composable
private fun ResumenEstadia(detalle: ReservaDetalleCliente) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Section(detalle.tipoHabitacion ?: "Reserva ${detalle.numeroReserva}")
        Text(detalle.estado.lowercase().replaceFirstChar(Char::uppercase))
        DatoEstadia("Reserva", detalle.numeroReserva)
        detalle.habitacion?.let { DatoEstadia("Habitación", it) }
        DatoEstadia("Entrada", fechaVisible(detalle.fechaInicio))
        DatoEstadia("Salida", fechaVisible(detalle.fechaFin))
        DatoEstadia("Huéspedes", detalle.cantidadPersonas.toString())
        detalle.precioNoche?.let { DatoEstadia("Por noche", moneda(it)) }
        if (detalle.cuenta == null) detalle.total?.let { DatoEstadia("Alojamiento", moneda(it)) }
    }
}

@Composable
private fun SaldoCuenta(cuenta: CuentaDetalleCliente, acciones: @Composable ColumnScope.() -> Unit = {}) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (cuenta.saldo.signum() > 0) "Saldo pendiente" else "Cuenta al día")
            Section(moneda(cuenta.saldo))
            acciones()
        }
    }
}

@Composable
private fun CargosCuenta(alojamiento: BigDecimal?, cuenta: CuentaDetalleCliente) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Section("Cargos")
        alojamiento?.let { DatoEstadia("Alojamiento", moneda(it)) }
        cuenta.consumos.orEmpty().forEach { consumo ->
            HorizontalDivider()
            DatoEstadia("${consumo.servicio} (cantidad: ${consumo.cantidad})",
                moneda(consumo.precio.multiply(consumo.cantidad.toBigDecimal())))
        }
        HorizontalDivider()
        DatoEstadia("Total de cargos", moneda(cuenta.total))
    }
}

@Composable
private fun PagosRegistrados(cuenta: CuentaDetalleCliente) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Section("Pagos registrados")
        val pagos = cuenta.pagos.orEmpty()
        if (pagos.isEmpty()) Text("Aún no hay pagos en esta cuenta.")
        pagos.forEachIndexed { index, pago ->
            if (index > 0) HorizontalDivider()
            DatoEstadia(pago.metodoPago, moneda(pago.monto))
            Text("${pago.fechaPago.replace('T', ' ')}, ${pago.estado}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DatoEstadia(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(etiqueta, Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.labelLarge)
    }
}
