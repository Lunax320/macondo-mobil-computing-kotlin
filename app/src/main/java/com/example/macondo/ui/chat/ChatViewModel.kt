package com.example.macondo.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.macondo.data.AccionChat
import com.example.macondo.data.LIMITE_HISTORIAL_CHAT
import com.example.macondo.data.LIMITE_MENSAJE_CHAT
import com.example.macondo.data.MensajeChatApi
import com.example.macondo.data.SolicitudChat
import com.example.macondo.data.remote.ChatApi
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

internal data class TurnoChat(
    val pregunta: String,
    val respuesta: String,
    val acciones: List<AccionChat>
)

internal data class EstadoChat(
    val borrador: String = "",
    val turnos: List<TurnoChat> = emptyList(),
    val pendiente: String? = null,
    val error: String? = null
) {
    val enviando get() = pendiente != null && error == null
}

internal class ChatViewModel(private val api: ChatApi) : ViewModel() {
    private val actual = MutableStateFlow(EstadoChat())
    val estado = actual.asStateFlow()
    private var peticion: Job? = null
    private var usuarioId: Long? = null

    fun cambiarUsuario(id: Long?) {
        if (usuarioId == id) return
        usuarioId = id
        limpiar()
    }

    fun limpiar() {
        peticion?.cancel()
        actual.value = EstadoChat()
    }

    fun editar(texto: String) {
        actual.value = actual.value.copy(borrador = texto.limitarParaChat())
    }

    fun enviar(texto: String = actual.value.borrador) {
        val estado = actual.value
        val pregunta = texto.trim().limitarParaChat()
        if (estado.enviando || pregunta.isBlank()) return
        actual.value = estado.copy(borrador = "", pendiente = pregunta, error = null)
        consultar(pregunta)
    }

    fun reintentar() {
        val estado = actual.value
        val pregunta = estado.pendiente ?: return
        if (estado.enviando) return
        actual.value = estado.copy(error = null)
        consultar(pregunta)
    }

    private fun consultar(pregunta: String) {
        val historial = actual.value.turnos.takeLast(LIMITE_HISTORIAL_CHAT / 2).flatMap { turno ->
            listOf(
                MensajeChatApi("user", turno.pregunta),
                MensajeChatApi("assistant", turno.respuesta.limitarParaChat())
            )
        }
        peticion = viewModelScope.launch {
            try {
                val respuesta = api.conversar(SolicitudChat(pregunta, historial))
                val texto = respuesta.respuesta?.trim()?.takeIf { it.isNotEmpty() }
                    ?: error("Respuesta de chat vacía")
                val turno = TurnoChat(
                    pregunta, texto, respuesta.acciones.orEmpty().mapNotNull(AccionChat::desdeId).distinct()
                )
                actual.value = actual.value.let {
                    it.copy(turnos = (it.turnos + turno).takeLast(MAX_TURNOS), pendiente = null)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                actual.value = actual.value.copy(error = error.mensajeChat())
            }
        }
    }

    private companion object {
        const val MAX_TURNOS = 50
    }
}

private fun String.limitarParaChat(): String =
    if (length <= LIMITE_MENSAJE_CHAT) this else take(LIMITE_MENSAJE_CHAT).trimEnd { it.isHighSurrogate() }

private fun Exception.mensajeChat(): String = when {
    this is HttpException && code() in setOf(401, 403, 404, 405) ->
        "El asistente no está disponible en este momento."
    this is HttpException && code() == 429 ->
        "El asistente está ocupado. Inténtalo de nuevo en unos segundos."
    this is IOException -> "No se pudo conectar con el asistente. Comprueba tu conexión e inténtalo de nuevo."
    else -> "El asistente no pudo responder. Inténtalo de nuevo en un momento."
}
