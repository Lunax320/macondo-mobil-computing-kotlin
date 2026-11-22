package com.example.macondo.data.remote

import com.google.gson.JsonParser
import java.io.IOException
import retrofit2.HttpException

fun Exception.mensajeParaUsuario(): String = when (this) {
    is HttpException -> {
        val mensaje = runCatching {
            JsonParser.parseString(response()?.errorBody()?.string().orEmpty())
                .asJsonObject.get("mensaje").asString
        }.getOrNull()
        mensaje ?: when (code()) {
            401 -> "Inicia sesión para continuar."
            403 -> "No tienes permiso para realizar esta operación."
            404 -> "El recurso no está disponible."
            in 500..599 -> "El servidor no está disponible en este momento."
            else -> "No se pudo completar la solicitud."
        }
    }
    is IOException -> "No se pudo conectar con el hotel. Comprueba la conexión e inténtalo de nuevo."
    else -> "No se pudo completar la operación."
}
