package com.example.macondo.data.remote

import com.example.macondo.data.RespuestaChat
import com.example.macondo.data.SolicitudChat
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatApi {
    @POST("chat")
    suspend fun conversar(@Body solicitud: SolicitudChat): RespuestaChat
}
