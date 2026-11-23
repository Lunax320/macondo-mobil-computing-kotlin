package com.example.macondo.data

import java.math.BigDecimal

data class ReservaDetalleCliente(
    val id: Long,
    val numeroReserva: String,
    val fechaInicio: String,
    val fechaFin: String,
    val cantidadPersonas: Int,
    val estado: String,
    val precioNoche: BigDecimal?,
    val total: BigDecimal?,
    val habitacion: String?,
    val tipoHabitacion: String?,
    val cuenta: CuentaDetalleCliente?
)

data class CuentaDetalleCliente(
    val id: Long,
    val estado: String,
    val total: BigDecimal,
    val saldo: BigDecimal,
    val consumos: List<Consumo>,
    val pagos: List<Pago>
)

data class PagoWompiSolicitud(val monto: BigDecimal)

data class CheckoutWompi(
    val referencia: String,
    val checkoutUrl: String,
    val monto: BigDecimal,
    val estado: String
)
