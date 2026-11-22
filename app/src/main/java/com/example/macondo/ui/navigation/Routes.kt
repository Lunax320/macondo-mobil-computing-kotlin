package com.example.macondo.ui.navigation

import android.net.Uri

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
    const val perfil = "cliente/{id}/perfil"

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
    fun perfil(id: Long) = "cliente/$id/perfil"
}
