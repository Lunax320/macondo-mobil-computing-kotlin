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
}

