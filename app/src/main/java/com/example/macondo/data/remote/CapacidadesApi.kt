package com.example.macondo.data.remote

import com.google.gson.JsonObject
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

data class CapacidadesApi(
    val detalleReserva: Boolean = false,
    val wompi: Boolean = false,
    val verificacionCorreo: Boolean = false,
    val recuperarContrasena: Boolean = false,
    val promocionesPublicas: Boolean = false,
    val calcularPromocion: Boolean = false,
    val administrarPromociones: Boolean = false
)

class DescubrimientoApi @Inject constructor(private val api: CatalogApi) {
    suspend fun consultar(): CapacidadesApi = try {
        val rutas = api.contrato().getAsJsonObject("paths")
        fun expone(metodo: String, ruta: String) = rutas?.getAsJsonObject("/api/$ruta")?.has(metodo) == true

        CapacidadesApi(
            detalleReserva = expone("get", "reserva/cliente/{id}/{reservaId}/detalle"),
            wompi = expone("post", "reserva/cliente/{id}/{reservaId}/pago/wompi"),
            verificacionCorreo = expone("post", "auth/verificacion/reenviar"),
            recuperarContrasena = expone("post", "auth/contrasena/solicitar-recuperacion"),
            promocionesPublicas = expone("get", "promociones/publicas"),
            calcularPromocion = expone("get", "promociones/calcular"),
            administrarPromociones = expone("get", "promociones") && expone("post", "promociones") &&
                expone("put", "promociones/{id}") && expone("delete", "promociones/{id}")
        )
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        CapacidadesApi()
    }
}
