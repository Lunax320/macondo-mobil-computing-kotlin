package com.example.macondo.data

import java.math.BigDecimal

enum class TipoDescuento { PORCENTAJE, VALOR_FIJO }

data class Promocion(
    val id: Long? = null,
    val codigo: String,
    val nombre: String,
    val descripcion: String?,
    val tipoDescuento: TipoDescuento,
    val valorDescuento: BigDecimal,
    val fechaInicio: String,
    val fechaFin: String,
    val nochesMinimas: Int,
    val activa: Boolean
)

data class PromocionAplicada(
    val codigo: String?,
    val nombre: String?,
    val subtotalEstadia: BigDecimal,
    val descuento: BigDecimal,
    val total: BigDecimal
)
