package com.example.macondo.ui.navigation

import com.example.macondo.data.AccionChat
import com.example.macondo.data.Sesion

internal data class DestinoChat(val etiqueta: String, val ruta: String)

internal fun AccionChat.destino(sesion: Sesion?): DestinoChat? {
    val personal = sesion?.tiene("ADMINISTRADOR", "OPERADOR") == true
    val clienteId = sesion?.takeIf { it.tiene("CLIENTE") }?.cliente?.id
    return when (this) {
        AccionChat.RESERVAR -> DestinoChat("Ver habitaciones", Routes.habitaciones)
        AccionChat.REGISTRO -> if (sesion == null) DestinoChat("Crear cuenta", Routes.registro) else null
        AccionChat.CANCELAR_RESERVA -> when {
            personal -> DestinoChat("Ver reservas", Routes.operadorReservas)
            clienteId != null -> DestinoChat("Gestionar mis reservas", Routes.activas(clienteId))
            sesion == null -> DestinoChat("Iniciar sesión para cancelar", Routes.login(accion = this))
            else -> null
        }
        AccionChat.CHECKOUT, AccionChat.PAGO_SEGURO -> when {
            personal -> DestinoChat("Abrir checkout", Routes.checkout())
            clienteId != null -> DestinoChat("Ver mis reservas y pagos", Routes.activas(clienteId))
            sesion == null -> DestinoChat("Iniciar sesión para ver reservas y pagos", Routes.login(accion = this))
            else -> null
        }
    }
}
