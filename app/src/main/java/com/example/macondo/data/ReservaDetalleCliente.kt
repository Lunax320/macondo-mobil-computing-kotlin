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
    val cuenta: CuentaDetalleCliente?,
    val codigoPromocional: String? = null,
    val descuentoPromocional: BigDecimal? = null
)

data class CuentaDetalleCliente(
    val id: Long,
    val estado: String,
    val total: BigDecimal,
    val saldo: BigDecimal,
    val consumos: List<Consumo>,
    val pagos: List<Pago>
)

enum class ProveedorPago(val nombre: String) {
    WOMPI("Wompi"),
    MERCADO_PAGO("Mercado Pago");
}

data class PagoCheckoutSolicitud(val monto: BigDecimal)

data class CheckoutPago(
    val referencia: String,
    val checkoutUrl: String,
    val monto: BigDecimal,
    val estado: String
)
