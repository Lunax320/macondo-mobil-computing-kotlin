package com.example.macondo

import com.example.macondo.data.MensajeChatApi
import com.example.macondo.data.ReservaSolicitud
import com.example.macondo.data.SolicitudChat
import com.example.macondo.data.remote.AccountApi
import com.example.macondo.data.remote.ChatApi
import com.example.macondo.data.remote.ReservationApi
import com.google.gson.JsonParser
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ContratoApiTest {
    @Test
    fun chatEnviaElContextoYLeeLasAccionesDelBackend() = runBlocking {
        val captura = AtomicReference<Request>()
        val api = retrofit(captura, 200, """{"respuesta":"Abre Mis reservas","acciones":["PAGO_SEGURO"]}""")
            .create(ChatApi::class.java)

        val respuesta = api.conversar(SolicitudChat(
            "¿Cómo pago?",
            listOf(MensajeChatApi("user", "Necesito ayuda con mi reserva"))
        ))

        val peticion = captura.get()
        assertEquals("POST", peticion.method)
        assertEquals("/api/chat", peticion.url.encodedPath)
        val buffer = Buffer()
        requireNotNull(peticion.body).writeTo(buffer)
        val json = JsonParser.parseString(buffer.readUtf8()).asJsonObject
        assertEquals("¿Cómo pago?", json.get("mensaje").asString)
        val mensaje = json.getAsJsonArray("historial").single().asJsonObject
        assertEquals("user", mensaje.get("role").asString)
        assertEquals("Necesito ayuda con mi reserva", mensaje.get("content").asString)
        assertEquals("Abre Mis reservas", respuesta.respuesta)
        assertEquals(listOf("PAGO_SEGURO"), respuesta.acciones)
    }

    @Test
    fun disponibilidadDistingueFechasDeConsultaYExcluyeLaReservaEditada() = runBlocking {
        val captura = AtomicReference<Request>()
        val api = retrofit(captura, 200, """[{"id":3,"numero":"103","capacidad":2}]""")
            .create(ReservationApi::class.java)

        val unidades = api.disponibilidad(7, 2, "2026-12-10", "2026-12-13", 2, 12)

        val peticion = captura.get()
        assertEquals("GET", peticion.method)
        assertEquals("/api/reserva/cliente/7/disponibilidad", peticion.url.encodedPath)
        assertEquals("2026-12-10", peticion.url.queryParameter("fechaEntrada"))
        assertEquals("2026-12-13", peticion.url.queryParameter("fechaSalida"))
        assertEquals("2", peticion.url.queryParameter("cantidadPersonas"))
        assertEquals("2", peticion.url.queryParameter("tipoId"))
        assertEquals("12", peticion.url.queryParameter("reservaId"))
        assertEquals("103", unidades.single().numero)
    }

    @Test
    fun altaEnviaElDtoEditableYLeeLaHabitacionAsignada() = runBlocking {
        val captura = AtomicReference<Request>()
        val api = retrofit(captura, 201, """
            {"id":12,"numeroReserva":"R-12","fechaInicio":"2026-12-10","fechaFin":"2026-12-13",
             "cantidadPersonas":2,"estado":"CONFIRMADA","total":1200000.00,
             "habitacion":{"id":3,"numero":"103","tipoHabitacion":{"id":2,"nombre":"Superior"}}}
        """.trimIndent()).create(ReservationApi::class.java)

        val reserva = api.crear(7, 2, ReservaSolicitud("2026-12-10", "2026-12-13", 2))

        val peticion = captura.get()
        assertEquals("/api/reserva/cliente/7", peticion.url.encodedPath)
        assertEquals("2", peticion.url.queryParameter("tipoId"))
        val buffer = Buffer()
        requireNotNull(peticion.body).writeTo(buffer)
        val json = JsonParser.parseString(buffer.readUtf8()).asJsonObject
        assertEquals("2026-12-10", json.get("fechaInicio").asString)
        assertEquals("2026-12-13", json.get("fechaFin").asString)
        assertFalse(json.has("tipoId"))
        assertEquals("R-12", reserva.numeroReserva)
        assertEquals(2L, reserva.habitacion?.tipoHabitacion?.id)
    }

    @Test
    fun eliminarComentarioAceptaRespuesta204SinCuerpo() = runBlocking {
        val captura = AtomicReference<Request>()
        val api = retrofit(captura, 204, "").create(AccountApi::class.java)

        api.borrarComentario(3, 7)

        assertEquals("DELETE", captura.get().method)
        assertEquals("/api/comentario/3", captura.get().url.encodedPath)
        assertEquals("7", captura.get().url.queryParameter("clienteId"))
    }

    private fun retrofit(captura: AtomicReference<Request>, status: Int, json: String): Retrofit {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request().also(captura::set)
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(status).message("Respuesta de prueba")
                .body(json.toResponseBody("application/json".toMediaType())).build()
        }.build()

        return Retrofit.Builder().baseUrl("https://macondo.test/api/")
            .client(client).addConverterFactory(GsonConverterFactory.create()).build()
    }
}
