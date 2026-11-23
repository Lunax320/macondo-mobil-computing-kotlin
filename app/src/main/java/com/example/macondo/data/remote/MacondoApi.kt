package com.example.macondo.data.remote

import com.example.macondo.data.*
import com.google.gson.JsonObject
import retrofit2.http.*

interface CatalogApi {
    @GET("../v3/api-docs")
    suspend fun contrato(): JsonObject

    @GET("tipo-habitacion")
    suspend fun tipos(): List<TipoHabitacion>

    @GET("tipo-habitacion/find/{id}")
    suspend fun tipo(@Path("id") id: Long): TipoHabitacion

    @GET("servicio/catalogo")
    suspend fun servicios(): List<Servicio>

    @GET("servicio/find/{id}")
    suspend fun servicio(@Path("id") id: Long): Servicio

    @GET("servicio/recomendaciones")
    suspend fun recomendaciones(): List<Servicio>

    @GET("servicio/relacionados/{id}")
    suspend fun relacionados(@Path("id") id: Long): List<Servicio>

    @GET("testimonio")
    suspend fun testimonios(): List<Testimonio>

    @GET("comentario/publicados")
    suspend fun publicados(): List<Comentario>
}

interface AccountApi {
    @POST("auth/login")
    suspend fun login(@Body solicitud: LoginSolicitud): Sesion

    @POST("auth/verificacion/reenviar")
    suspend fun reenviarVerificacion(@Body solicitud: CorreoSolicitud)

    @POST("auth/contrasena/solicitar-recuperacion")
    suspend fun solicitarRecuperacion(@Body solicitud: CorreoSolicitud): MensajeCuenta

    @POST("cliente/registro")
    suspend fun registro(@Body solicitud: RegistroSolicitud): Cliente

    @GET("cliente/find/{id}")
    suspend fun perfil(@Path("id") id: Long): Cliente

    @POST("cliente/{id}/perfil")
    suspend fun actualizarPerfil(@Path("id") id: Long, @Body cliente: Cliente): Cliente

    @POST("cliente/{id}/password")
    suspend fun clave(@Path("id") id: Long, @Body solicitud: ClaveSolicitud)

    @POST("cliente/delete/{id}")
    suspend fun borrarCuenta(@Path("id") id: Long)

    @GET("comentario/cliente/{id}")
    suspend fun comentarios(@Path("id") id: Long): List<Comentario>

    @POST("comentario")
    suspend fun comentar(@Body solicitud: ComentarioSolicitud): Comentario

    @PUT("comentario/{id}")
    suspend fun editarComentario(@Path("id") id: Long, @Query("clienteId") clienteId: Long, @Body solicitud: ComentarioEdicion): Comentario

    @DELETE("comentario/{id}")
    suspend fun borrarComentario(@Path("id") id: Long, @Query("clienteId") clienteId: Long)
}

interface ReservationApi {
    @GET("reserva/cliente/{id}/{reservaId}/detalle")
    suspend fun detalleCliente(@Path("id") id: Long, @Path("reservaId") reservaId: Long): ReservaDetalleCliente

    @GET("reserva/cliente/{id}/activas")
    suspend fun activas(@Path("id") id: Long): List<Reserva>

    @GET("reserva/cliente/{id}/historial")
    suspend fun historial(@Path("id") id: Long): List<Reserva>

    @GET("reserva/cliente/{id}/pagos")
    suspend fun pagos(@Path("id") id: Long): List<Pago>

    @GET("reserva/cliente/{id}/disponibilidad")
    suspend fun disponibilidad(
        @Path("id") clienteId: Long, @Query("tipoId") tipoId: Long,
        @Query("fechaEntrada") entrada: String, @Query("fechaSalida") salida: String,
        @Query("cantidadPersonas") personas: Int, @Query("reservaId") reservaId: Long?
    ): List<Habitacion>

    @GET("reserva/cliente/{id}/find/{reservaId}")
    suspend fun editable(@Path("id") id: Long, @Path("reservaId") reservaId: Long): Reserva

    @POST("reserva/cliente/{id}")
    suspend fun crear(@Path("id") id: Long, @Query("tipoId") tipoId: Long, @Body solicitud: ReservaSolicitud): Reserva

    @PUT("reserva/cliente/{id}/{reservaId}")
    suspend fun editar(
        @Path("id") id: Long, @Path("reservaId") reservaId: Long,
        @Query("tipoId") tipoId: Long, @Body solicitud: ReservaSolicitud
    ): Reserva

    @PUT("reserva/cliente/{id}/{reservaId}/cancelar")
    suspend fun cancelar(@Path("id") id: Long, @Path("reservaId") reservaId: Long): Reserva
}

interface OperationsApi {
    @GET("reserva")
    suspend fun reservas(): List<Reserva>

    @GET("reserva/{numero}")
    suspend fun detalle(@Path("numero") numero: String): Reserva

    @GET("reserva/id/{id}")
    suspend fun detallePorId(@Path("id") id: Long): Reserva

    @GET("reserva/habitacion/{numero}/actual")
    suspend fun cuentaActual(@Path("numero") numero: String): ReservaIdentificador

    @GET("reserva/habitacion/{numero}/checkout")
    suspend fun checkout(@Path("numero") numero: String): ReservaIdentificador

    @PUT("reserva/{numero}/cancelar")
    suspend fun cancelar(@Path("numero") numero: String): Reserva

    @PUT("reserva/{numero}/activar")
    suspend fun activar(@Path("numero") numero: String): Reserva

    @PUT("reserva/{numero}/finalizar")
    suspend fun finalizar(@Path("numero") numero: String): Reserva

    @POST("operador/cuenta/reserva/{numero}/servicios")
    suspend fun agregarConsumos(@Path("numero") numero: String, @Body consumos: List<ConsumoSolicitud>)

    @DELETE("operador/cuenta/reserva/{numero}/servicios/{id}")
    suspend fun eliminarConsumo(@Path("numero") numero: String, @Path("id") id: Long)

    @POST("operador/cuenta/reserva/{numero}/pago")
    suspend fun pagar(@Path("numero") numero: String, @Body solicitud: PagoSolicitud): Pago

    @GET("habitacion/reserva/{id}")
    suspend fun unidadDeReserva(@Path("id") id: Long): Habitacion
}

interface AdminApi {
    @GET("habitacion")
    suspend fun habitaciones(): List<Habitacion>

    @GET("habitacion/find/{id}")
    suspend fun habitacion(@Path("id") id: Long): Habitacion

    @POST("habitacion")
    suspend fun crearHabitacion(@Body habitacion: HabitacionSolicitud): Habitacion

    @PUT("habitacion")
    suspend fun editarHabitacion(@Body habitacion: HabitacionSolicitud): Habitacion

    @PUT("habitacion/estado/{id}")
    suspend fun cambiarHabitacion(@Path("id") id: Long): Habitacion

    @DELETE("habitacion/delete/{id}")
    suspend fun borrarHabitacion(@Path("id") id: Long)

    @POST("tipo-habitacion")
    suspend fun crearTipo(@Body tipo: TipoHabitacion): TipoHabitacion

    @PUT("tipo-habitacion")
    suspend fun editarTipo(@Body tipo: TipoHabitacion): TipoHabitacion

    @DELETE("tipo-habitacion/delete/{id}")
    suspend fun borrarTipo(@Path("id") id: Long)

    @GET("servicio")
    suspend fun servicios(): List<Servicio>

    @POST("servicio")
    suspend fun crearServicio(@Body servicio: Servicio): Servicio

    @PUT("servicio")
    suspend fun editarServicio(@Body servicio: Servicio): Servicio

    @PUT("servicio/estado/{id}")
    suspend fun cambiarServicio(@Path("id") id: Long): Servicio

    @GET("operador")
    suspend fun operadores(): List<Operador>

    @POST("operador")
    suspend fun crearOperador(@Body operador: OperadorSolicitud): Operador

    @PUT("operador/estado/{id}")
    suspend fun cambiarOperador(@Path("id") id: Long): Operador

    @DELETE("operador/delete/{id}")
    suspend fun borrarOperador(@Path("id") id: Long)

    @PUT("comentario/{id}/publicar")
    suspend fun publicarComentario(@Path("id") id: Long): Comentario
}
