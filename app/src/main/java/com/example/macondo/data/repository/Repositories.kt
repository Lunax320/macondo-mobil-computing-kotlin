package com.example.macondo.data.repository

import com.example.macondo.data.*
import com.example.macondo.data.remote.*
import com.example.macondo.data.session.SessionStore
import javax.inject.Inject
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException

data class InicioHotel(
    val tipos: List<TipoHabitacion>,
    val servicios: List<Servicio>,
    val testimonios: List<Testimonio>,
    val comentarios: List<Comentario>?
)

data class DetalleServicio(val servicio: Servicio, val relacionados: List<Servicio>)
data class HistorialCliente(val reservas: List<Reserva>, val comentarios: Map<Long, Comentario>?)
data class FormularioReserva(val tipos: List<TipoHabitacion>, val reserva: Reserva?)

class PromocionesRepository @Inject constructor(private val api: PromocionesApi) : PromocionesApi by api {
    suspend fun guardar(promocion: Promocion) = promocion.id?.let { api.actualizar(it, promocion) }
        ?: api.crear(promocion)
}

private suspend fun <T> opcionComentarios(operacion: suspend () -> T): T? = try {
    operacion()
} catch (error: CancellationException) {
    throw error
} catch (error: HttpException) {
    if (error.code() == 403 || error.code() == 404) null else throw error
}

class CatalogRepository @Inject constructor(private val api: CatalogApi) : CatalogApi by api {
    suspend fun inicio(): InicioHotel = coroutineScope {
        val tipos = async { api.tipos() }
        val servicios = async { api.recomendaciones() }
        val testimonios = async { api.testimonios() }
        val comentarios = async { opcionComentarios { api.publicados() } }
        InicioHotel(tipos.await(), servicios.await(), testimonios.await(), comentarios.await())
    }

    suspend fun detalleServicio(id: Long): DetalleServicio = coroutineScope {
        val servicio = async { api.servicio(id) }
        val relacionados = async { api.relacionados(id) }
        DetalleServicio(servicio.await(), relacionados.await())
    }
}

class AccountRepository @Inject constructor(
    private val api: AccountApi,
    private val sesiones: SessionStore
) : AccountApi by api {
    val sesion = sesiones.sesion

    suspend fun restaurar() = withContext(Dispatchers.IO) { sesiones.restaurar() }

    suspend fun iniciar(solicitud: LoginSolicitud) {
        val sesion = api.login(solicitud)
        withContext(Dispatchers.IO) { sesiones.guardar(sesion) }
    }

    override suspend fun actualizarPerfil(id: Long, cliente: Cliente): Cliente {
        val token = sesion.value?.token
        val perfil = api.actualizarPerfil(id, cliente)
        if (token != null) withContext(Dispatchers.IO) { sesiones.actualizarPerfil(token, perfil) }
        return perfil
    }

    suspend fun salir() = withContext(Dispatchers.IO) { sesiones.cerrar() }
}

class ReservationRepository @Inject constructor(
    private val api: ReservationApi,
    private val cuentas: AccountApi,
    private val catalogo: CatalogApi
) : ReservationApi by api {
    suspend fun detalle(clienteId: Long, reservaId: Long, historial: Boolean, cuentaDisponible: Boolean): ReservaDetalleCliente {
        val detalle = if (cuentaDisponible) api.detalleCliente(clienteId, reservaId) else null
        if (detalle?.descuentoPromocional != null) return detalle
        val reserva = try {
            val reservas = if (historial) api.historial(clienteId) else api.activas(clienteId)
            reservas.firstOrNull { it.id == reservaId }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (detalle != null) return detalle
            throw error
        }
        if (detalle != null) {
            return detalle.copy(
                codigoPromocional = detalle.codigoPromocional ?: reserva?.codigoPromocional,
                descuentoPromocional = reserva?.descuentoPromocional
            )
        }
        reserva
            ?: throw IllegalStateException("La reserva ya no aparece en este listado.")
        return ReservaDetalleCliente(
            reserva.id, reserva.numeroReserva, reserva.fechaInicio, reserva.fechaFin, reserva.cantidadPersonas,
            reserva.estado, reserva.precioNoche, reserva.total, reserva.habitacion?.numero,
            reserva.habitacion?.tipoHabitacion?.nombre, null, reserva.codigoPromocional, reserva.descuentoPromocional
        )
    }

    suspend fun iniciarPago(proveedor: ProveedorPago, clienteId: Long, reservaId: Long, monto: BigDecimal): CheckoutPago {
        val solicitud = PagoCheckoutSolicitud(monto)
        return when (proveedor) {
            ProveedorPago.WOMPI -> api.iniciarWompi(clienteId, reservaId, solicitud)
            ProveedorPago.MERCADO_PAGO -> api.iniciarMercadoPago(clienteId, reservaId, solicitud)
        }
    }

    suspend fun formulario(clienteId: Long, reservaId: Long?): FormularioReserva = coroutineScope {
        val tipos = async { catalogo.tipos() }
        val reserva = reservaId?.let { id -> async { api.editable(clienteId, id) } }
        FormularioReserva(tipos.await(), reserva?.await())
    }

    suspend fun historialConComentarios(clienteId: Long): HistorialCliente = coroutineScope {
        val reservas = async { api.historial(clienteId) }
        val comentarios = async {
            opcionComentarios { cuentas.comentarios(clienteId) }?.mapNotNull { comentario ->
                comentario.reservaId?.let { it to comentario }
            }?.toMap()
        }
        HistorialCliente(reservas.await(), comentarios.await())
    }

    suspend fun buscarDisponibilidad(clienteId: Long, tipoId: Long, datos: ReservaSolicitud, reservaId: Long?) =
        api.disponibilidad(clienteId, tipoId, datos.fechaInicio, datos.fechaFin, datos.cantidadPersonas, reservaId)

    suspend fun guardar(clienteId: Long, tipoId: Long, datos: ReservaSolicitud, reservaId: Long?) =
        if (reservaId == null) api.crear(clienteId, tipoId, datos)
        else api.editar(clienteId, reservaId, tipoId, datos)
}

data class CuentaOperativa(val reserva: Reserva, val servicios: List<Servicio>)
class OperationsRepository @Inject constructor(
    private val api: OperationsApi,
    private val catalogo: CatalogApi
) : OperationsApi by api {
    suspend fun cuenta(id: Long): CuentaOperativa = coroutineScope {
        val reserva = async { api.detallePorId(id) }
        val servicios = async { catalogo.servicios() }
        CuentaOperativa(reserva.await(), servicios.await())
    }
}

class AdminRepository @Inject constructor(private val api: AdminApi) : AdminApi by api {
    suspend fun guardar(habitacion: HabitacionSolicitud) =
        if (habitacion.id == null) api.crearHabitacion(habitacion) else api.editarHabitacion(habitacion)

    suspend fun guardar(tipo: TipoHabitacion) =
        if (tipo.id == null) api.crearTipo(tipo) else api.editarTipo(tipo)

    suspend fun guardar(servicio: Servicio) =
        if (servicio.id == null) api.crearServicio(servicio) else api.editarServicio(servicio)
}
