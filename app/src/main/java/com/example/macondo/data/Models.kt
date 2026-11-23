package com.example.macondo.data

import java.math.BigDecimal

data class TipoHabitacion(
    val id: Long? = null, val nombre: String, val descripcion: String? = null,
    val imagen: String? = null, val precioNoche: BigDecimal, val capacidadPersonas: Int
)

data class TipoResumen(val id: Long, val nombre: String, val imagen: String?, val precioNoche: BigDecimal?)

data class Habitacion(
    val id: Long, val nombre: String, val numero: String, val estado: String, val capacidad: Int,
    val piso: Int?, val etiqueta: String?, val precio: BigDecimal?, val tipoHabitacion: TipoResumen?
)

data class IdTipo(val id: Long)

data class HabitacionSolicitud(
    val id: Long? = null, val tipoHabitacion: IdTipo, val nombre: String, val etiqueta: String?,
    val numero: String, val piso: Int, val capacidad: Int, val estado: String
)

data class Servicio(
    val id: Long? = null, val nombre: String, val descripcion: String? = null,
    val categoria: String? = null, val imagen: String? = null, val destacado: Boolean = false,
    val precio: BigDecimal, val activo: Boolean = true, val duracion: String? = null,
    val descripcionDetalle: String? = null, val horario: String? = null,
    val incluidos: List<String> = emptyList(), val etiquetas: List<String> = emptyList()
)

data class Testimonio(
    val id: Long, val nombre: String, val texto: String?, val ciudad: String?, val imagen: String?, val estrellas: Int
)

data class Cliente(
    val id: Long? = null, val nombre: String, val apellido: String,
    val cedula: String, val telefono: String, val correo: String, val mensaje: String? = null
) {
    val nombreCompleto get() = "$nombre $apellido".trim()
}

data class LoginSolicitud(val correo: String, val contrasena: String)
data class CorreoSolicitud(val correo: String)
data class MensajeCuenta(val mensaje: String)
data class RegistroSolicitud(val cliente: Cliente, val contrasena: String)
data class ClaveSolicitud(
    val contrasenaActual: String, val nuevaContrasena: String, val confirmarContrasena: String
)
data class Sesion(
    val id: Long, val correo: String, val rol: String?, val roles: List<String>,
    val cliente: Cliente?, val token: String
) {
    fun tiene(vararg autoridades: String) = autoridades.any { it == rol || it in roles }
}

data class ReservaSolicitud(
    val fechaInicio: String, val fechaFin: String, val cantidadPersonas: Int,
    val codigoPromocional: String? = null
)
data class Reserva(
    val id: Long, val numeroReserva: String, val fechaInicio: String, val fechaFin: String,
    val cantidadPersonas: Int, val estado: String, val precioNoche: BigDecimal?, val total: BigDecimal?,
    val habitacion: Habitacion?, val cliente: Cliente?, val cuenta: Cuenta?,
    val codigoPromocional: String? = null, val descuentoPromocional: BigDecimal? = null
)
data class ReservaIdentificador(val idReserva: Long)
data class Cuenta(val id: Long, val estado: String, val total: BigDecimal, val consumos: List<Consumo>, val pagos: List<Pago>)
data class Consumo(val id: Long, val cantidad: Int, val precio: BigDecimal, val servicio: String)
data class ConsumoSolicitud(val servicioId: Long, val cantidad: Int)
data class Pago(
    val id: Long, val numeroReserva: String?, val monto: BigDecimal,
    val metodoPago: String, val fechaPago: String, val estado: String
)
data class PagoSolicitud(val metodoPago: String)
data class Operador(val id: Long, val nombre: String, val activo: Boolean, val correo: String? = null)
data class OperadorCuentaSolicitud(val correo: String, val nombre: String, val contrasena: String)

data class Comentario(
    val id: Long, val reservaId: Long?, val nombre: String?, val calificacion: Int,
    val texto: String, val publicado: Boolean
)
data class ComentarioSolicitud(val clienteId: Long, val reservaId: Long, val calificacion: Int, val texto: String)
data class ComentarioEdicion(val calificacion: Int, val texto: String)
