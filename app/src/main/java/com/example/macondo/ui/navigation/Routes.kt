package com.example.macondo.ui.navigation

import android.net.Uri
import com.example.macondo.data.Sesion

internal object Routes {
    const val inicio = "inicio"
    const val login = "login?tipoId={tipoId}"
    const val registro = "registro"
    const val habitaciones = "habitaciones"
    const val habitacion = "habitaciones/{id}"
    const val servicios = "servicios"
    const val servicio = "servicios/{id}"
    const val explorar = "explorar"

    const val cliente = "cliente/{id}"
    const val activas = "cliente/{id}/reservas"
    const val historial = "cliente/{id}/historial"
    const val pagos = "cliente/{id}/pagos"
    const val nuevaReserva = "cliente/{id}/reservas/nueva?tipoId={tipoId}"
    const val editarReserva = "cliente/{id}/reservas/{reservaId}/editar"
    const val detalleReserva = "cliente/{id}/reservas/{reservaId}?historial={historial}"
    const val perfil = "cliente/{id}/perfil"

    const val operador = "operador"
    const val operadorReservas = "operador/reservas"
    const val operadorDetalle = "operador/reservas/{numeroReserva}"
    const val buscarCuenta = "operador/cuenta"
    const val cuenta = "operador/cuenta/{reservaId}"
    const val checkout = "operador/checkout?reservaId={reservaId}"

    const val admin = "admin"
    const val adminHabitaciones = "admin/habitaciones"
    const val adminNuevaHabitacion = "admin/habitaciones/nueva"
    const val adminEditarHabitacion = "admin/habitaciones/editar/{id}"
    const val adminTipos = "admin/tipos_habitacion"
    const val adminServicios = "admin/servicios"
    const val adminNuevoServicio = "admin/servicios/nuevo"
    const val adminEditarServicio = "admin/servicios/editar/{id}"
    const val adminOperadores = "admin/operadores"

    fun operadorDetalle(numero: String) = "operador/reservas/${Uri.encode(numero)}"
    fun cuenta(id: Long) = "operador/cuenta/$id"
    fun checkout(id: Long? = null) = id?.let { "operador/checkout?reservaId=$it" } ?: "operador/checkout"
    fun adminEditarHabitacion(id: Long) = "admin/habitaciones/editar/$id"
    fun adminEditarServicio(id: Long) = "admin/servicios/editar/$id"

    fun portal(sesion: Sesion?): String {
        if (sesion == null) return login()
        if (sesion.tiene("ADMINISTRADOR")) return admin
        if (sesion.tiene("OPERADOR")) return operador
        return sesion.cliente?.id?.let(::cliente) ?: inicio
    }

    fun protegida(ruta: String) =
        ruta.startsWith("cliente/") || ruta.startsWith("operador") || ruta.startsWith("admin")

    fun login(tipoId: Long? = null) = tipoId?.let { "login?tipoId=$it" } ?: "login"
    fun habitacion(id: Long) = "habitaciones/$id"
    fun servicio(id: Long) = "servicios/$id"
    fun cliente(id: Long) = "cliente/$id"
    fun activas(id: Long) = "cliente/$id/reservas"
    fun historial(id: Long) = "cliente/$id/historial"
    fun pagos(id: Long) = "cliente/$id/pagos"
    fun nuevaReserva(id: Long, tipoId: Long? = null) =
        "cliente/$id/reservas/nueva" + (tipoId?.let { "?tipoId=$it" } ?: "")
    fun editarReserva(id: Long, reservaId: Long) = "cliente/$id/reservas/$reservaId/editar"
    fun detalleReserva(id: Long, reservaId: Long, historial: Boolean = false) =
        "cliente/$id/reservas/$reservaId?historial=$historial"
    fun perfil(id: Long) = "cliente/$id/perfil"
}
