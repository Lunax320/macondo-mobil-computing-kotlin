package com.example.macondo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.macondo.data.CuentaDetalleCliente
import com.example.macondo.data.ReservaDetalleCliente
import com.example.macondo.data.repository.ReservationRepository
import com.example.macondo.ui.components.*
import com.example.macondo.ui.screenModel
import java.math.BigDecimal

@Composable
fun ReservaDetalleScreen(
    reservas: ReservationRepository, clienteId: Long, reservaId: Long,
    historial: Boolean, cuentaDisponible: Boolean
) {
    val model = screenModel("detalle:$clienteId:$reservaId:$historial:$cuentaDisponible") {
        reservas.detalle(clienteId, reservaId, historial, cuentaDisponible)
    }
    RemotePage("Detalle de reserva", model) { detalle, _ ->
        ResumenEstadia(detalle)
        detalle.cuenta?.let { cuenta ->
            SaldoCuenta(cuenta)
            CargosCuenta(detalle.total, cuenta)
            PagosRegistrados(cuenta)
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
        DatoEstadia("Entrada", detalle.fechaInicio)
        DatoEstadia("Salida", detalle.fechaFin)
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
            DatoEstadia("${consumo.cantidad} × ${consumo.servicio}", moneda(consumo.precio.multiply(consumo.cantidad.toBigDecimal())))
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
            Text("${pago.fechaPago.replace('T', ' ')} · ${pago.estado}", style = MaterialTheme.typography.bodySmall)
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
