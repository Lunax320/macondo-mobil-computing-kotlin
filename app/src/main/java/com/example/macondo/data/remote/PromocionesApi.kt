package com.example.macondo.data.remote

import com.example.macondo.data.Promocion
import com.example.macondo.data.PromocionAplicada
import java.math.BigDecimal
import retrofit2.http.*

interface PromocionesApi {
    @GET("promociones/publicas")
    suspend fun publicas(): List<Promocion>

    @GET("promociones/calcular")
    suspend fun calcular(
        @Query("codigo") codigo: String,
        @Query("fechaEntrada") fechaEntrada: String,
        @Query("fechaSalida") fechaSalida: String,
        @Query("precioNoche") precioNoche: BigDecimal
    ): PromocionAplicada

    @GET("promociones")
    suspend fun todas(): List<Promocion>

    @POST("promociones")
    suspend fun crear(@Body promocion: Promocion): Promocion

    @PUT("promociones/{id}")
    suspend fun actualizar(@Path("id") id: Long, @Body promocion: Promocion): Promocion

    @DELETE("promociones/{id}")
    suspend fun eliminar(@Path("id") id: Long)
}

