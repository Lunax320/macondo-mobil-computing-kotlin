package com.example.macondo.data

internal const val LIMITE_MENSAJE_CHAT = 1000
internal const val LIMITE_HISTORIAL_CHAT = 10

data class MensajeChatApi(val role: String, val content: String)
data class SolicitudChat(val mensaje: String, val historial: List<MensajeChatApi>)
data class RespuestaChat(val respuesta: String?, val acciones: List<String?>?)

internal enum class AccionChat {
    RESERVAR, REGISTRO, CANCELAR_RESERVA, CHECKOUT, PAGO_SEGURO;

    companion object {
        fun desdeId(id: String?) = entries.firstOrNull { it.name == id }
    }
}
