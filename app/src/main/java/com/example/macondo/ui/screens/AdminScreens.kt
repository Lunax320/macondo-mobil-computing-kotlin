package com.example.macondo.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import com.example.macondo.data.*
import com.example.macondo.data.repository.AdminRepository
import com.example.macondo.data.repository.CatalogRepository
import com.example.macondo.ui.components.*
import com.example.macondo.ui.screenModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Composable
fun AdminScreen(
    admin: AdminRepository,
    habitaciones: () -> Unit,
    tipos: () -> Unit,
    servicios: () -> Unit,
    operadores: () -> Unit,
    operacion: () -> Unit
) {
    val model = screenModel("admin-inicio") { Unit }
    var comentarioId by rememberSaveable { mutableStateOf("") }
    RemotePage("Administración", model) { _, estado ->
        Primary("Habitaciones físicas", onClick = habitaciones)
        Secondary("Tipos de habitación", onClick = tipos)
        Secondary("Servicios", onClick = servicios)
        Secondary("Operadores", onClick = operadores)
        Secondary("Abrir panel de operación", onClick = operacion)
        HorizontalDivider()
        Section("Moderación de comentarios")
        Field("ID del comentario", comentarioId, tipo = KeyboardType.Number) { comentarioId = it.filter(Char::isDigit) }
        Secondary("Publicar comentario", (comentarioId.toLongOrNull() ?: 0) > 0 && !estado.guardando) {
            val id = comentarioId.toLongOrNull() ?: return@Secondary
            model.ejecutar("Comentario publicado", recargar = false) { admin.publicarComentario(id) }
        }
    }
}

@Composable
fun AdminHabitacionesScreen(admin: AdminRepository, editar: (Long) -> Unit, nueva: () -> Unit) {
    val model = screenModel("admin-habitaciones", admin::habitaciones)
    var borrar by remember { mutableStateOf<Habitacion?>(null) }
    RemotePage("Habitaciones físicas", model) { habitaciones, estado ->
        Primary("Nueva habitación", onClick = nueva)
        if (habitaciones.isEmpty()) Text("No hay habitaciones registradas.")
        habitaciones.forEach { habitacion ->
            Panel {
                Section("${habitacion.numero} · ${habitacion.nombre}")
                Text("${habitacion.estado} · ${habitacion.capacidad} huéspedes")
                habitacion.tipoHabitacion?.let { Text(it.nombre) }
                Secondary("Editar") { editar(habitacion.id) }
                Secondary("Cambiar estado", !estado.guardando) {
                    model.ejecutar("Estado actualizado") { admin.cambiarHabitacion(habitacion.id) }
                }
                TextButton(onClick = { borrar = habitacion }, enabled = !estado.guardando) { Text("Eliminar") }
            }
        }
    }
    borrar?.let { habitacion ->
        Confirmar("Eliminar habitación", habitacion.nombre, "Eliminar", cerrar = { borrar = null }) {
            model.ejecutar("Habitación eliminada") { admin.borrarHabitacion(habitacion.id) }
        }
    }
}

@Composable
fun AdminHabitacionFormScreen(admin: AdminRepository, catalogo: CatalogRepository, id: Long?, volver: () -> Unit) {
    val model = screenModel("admin-habitacion:$id") {
        coroutineScope {
            val tipos = async { catalogo.tipos() }
            val habitacion = id?.let { async { admin.habitacion(it) } }
            tipos.await() to habitacion?.await()
        }
    }
    RemotePage(if (id == null) "Nueva habitación" else "Editar habitación", model) { datos, estado ->
        val (tipos, habitacion) = datos
        var tipoId by rememberSaveable(habitacion?.id) { mutableStateOf(habitacion?.tipoHabitacion?.id) }
        var nombre by rememberSaveable(habitacion?.id) { mutableStateOf(habitacion?.nombre.orEmpty()) }
        var numero by rememberSaveable(habitacion?.id) { mutableStateOf(habitacion?.numero.orEmpty()) }
        var etiqueta by rememberSaveable(habitacion?.id) { mutableStateOf(habitacion?.etiqueta.orEmpty()) }
        var capacidad by rememberSaveable(habitacion?.id) { mutableStateOf(habitacion?.capacidad?.toString().orEmpty()) }
        var piso by rememberSaveable(habitacion?.id) { mutableStateOf(habitacion?.piso?.toString() ?: "1") }
        var estadoHabitacion by rememberSaveable(habitacion?.id) { mutableStateOf(habitacion?.estado ?: "DISPONIBLE") }
        Selector("Tipo de habitación", tipos.firstOrNull { it.id == tipoId }, tipos, { it.nombre }) {
            tipoId = it.id
            capacidad = it.capacidadPersonas.toString()
        }
        Field("Nombre", nombre) { nombre = it }
        Field("Número", numero) { if (it.length <= 10) numero = it }
        Field("Piso", piso, tipo = KeyboardType.Number) { piso = it.filter(Char::isDigit) }
        Field("Capacidad", capacidad, tipo = KeyboardType.Number) { capacidad = it.filter(Char::isDigit) }
        Field("Etiqueta", etiqueta) { etiqueta = it }
        Selector("Estado", estadoHabitacion, listOf("DISPONIBLE", "MANTENIMIENTO"), { it }) { estadoHabitacion = it }
        val valido = tipoId != null && nombre.isNotBlank() && numero.isNotBlank() &&
            (piso.toIntOrNull() ?: 0) > 0 && (capacidad.toIntOrNull() ?: 0) > 0
        Primary("Guardar habitación", valido && !estado.guardando) {
            val tipo = tipoId ?: return@Primary
            val solicitud = HabitacionSolicitud(id, IdTipo(tipo), nombre.trim(), etiqueta.trim().ifBlank { null },
                numero.trim(), piso.toInt(), capacidad.toInt(), estadoHabitacion)
            model.ejecutar("Habitación guardada", recargar = false) {
                admin.guardar(solicitud)
                volver()
            }
        }
    }
}

@Composable
fun AdminTiposScreen(admin: AdminRepository, catalogo: CatalogRepository) {
    val model = screenModel("admin-tipos", catalogo::tipos)
    var editando by rememberSaveable { mutableStateOf(false) }
    var seleccionado by rememberSaveable { mutableStateOf<Long?>(null) }
    var borrar by remember { mutableStateOf<TipoHabitacion?>(null) }
    RemotePage("Tipos de habitación", model) { tipos, estado ->
        if (editando) {
            val tipo = tipos.firstOrNull { it.id == seleccionado }
            TipoForm(tipo, estado.guardando, cerrar = { editando = false }) { solicitud ->
                model.ejecutar("Tipo guardado") {
                    admin.guardar(solicitud)
                    editando = false
                }
            }
        } else {
            Primary("Nuevo tipo") { seleccionado = null; editando = true }
            if (tipos.isEmpty()) Text("No hay tipos registrados.")
            tipos.forEach { tipo ->
                Panel {
                    Section(tipo.nombre)
                    Text("${tipo.capacidadPersonas} huéspedes · ${moneda(tipo.precioNoche)}")
                    Secondary("Editar", !estado.guardando) { seleccionado = tipo.id; editando = true }
                    TextButton(onClick = { borrar = tipo }, enabled = !estado.guardando) { Text("Eliminar") }
                }
            }
        }
    }
    borrar?.let { tipo ->
        Confirmar("Eliminar tipo", tipo.nombre, "Eliminar", cerrar = { borrar = null }) {
            tipo.id?.let { id -> model.ejecutar("Tipo eliminado") { admin.borrarTipo(id) } }
        }
    }
}

@Composable
private fun TipoForm(tipo: TipoHabitacion?, guardando: Boolean, cerrar: () -> Unit, guardar: (TipoHabitacion) -> Unit) {
    var nombre by rememberSaveable(tipo?.id) { mutableStateOf(tipo?.nombre.orEmpty()) }
    var descripcion by rememberSaveable(tipo?.id) { mutableStateOf(tipo?.descripcion.orEmpty()) }
    var imagen by rememberSaveable(tipo?.id) { mutableStateOf(tipo?.imagen.orEmpty()) }
    var precio by rememberSaveable(tipo?.id) { mutableStateOf(tipo?.precioNoche?.toPlainString().orEmpty()) }
    var capacidad by rememberSaveable(tipo?.id) { mutableStateOf(tipo?.capacidadPersonas?.toString().orEmpty()) }

    Section(if (tipo == null) "Nuevo tipo" else "Editar tipo")
    Field("Nombre", nombre) { nombre = it }
    Field("Descripción", descripcion, multilinea = true) { descripcion = it }
    Field("Ruta o URL de imagen", imagen) { imagen = it }
    Field("Precio por noche", precio, tipo = KeyboardType.Decimal) { precio = it }
    Field("Capacidad", capacidad, tipo = KeyboardType.Number) { capacidad = it.filter(Char::isDigit) }
    val valor = precio.trim().replace(',', '.').toBigDecimalOrNull()
    val personas = capacidad.toIntOrNull()
    Primary("Guardar tipo", nombre.isNotBlank() && valor != null && valor.signum() >= 0 &&
        personas != null && personas > 0 && !guardando) {
        if (valor != null && personas != null) guardar(TipoHabitacion(tipo?.id, nombre.trim(),
            descripcion.trim(), imagen.trim(), valor, personas))
    }
    Secondary("Volver al listado", !guardando, cerrar)
}

@Composable
fun AdminServiciosScreen(admin: AdminRepository, editar: (Long) -> Unit, nuevo: () -> Unit) {
    val model = screenModel("admin-servicios", admin::servicios)
    RemotePage("Administrar servicios", model) { servicios, estado ->
        Primary("Nuevo servicio", onClick = nuevo)
        if (servicios.isEmpty()) Text("No hay servicios registrados.")
        servicios.forEach { servicio ->
            Panel {
                Section(servicio.nombre)
                Text("${if (servicio.activo) "Activo" else "Inactivo"} · ${moneda(servicio.precio)}")
                servicio.id?.let { id ->
                    Secondary("Editar") { editar(id) }
                    Secondary(if (servicio.activo) "Desactivar" else "Activar", !estado.guardando) {
                        model.ejecutar("Estado actualizado") { admin.cambiarServicio(id) }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminServicioFormScreen(admin: AdminRepository, catalogo: CatalogRepository, id: Long?, volver: () -> Unit) {
    val model = screenModel("admin-servicio:$id") { id?.let { catalogo.servicio(it) } ?: Servicio(nombre = "", precio = java.math.BigDecimal.ZERO) }
    RemotePage(if (id == null) "Nuevo servicio" else "Editar servicio", model) { servicio, estado ->
        var nombre by rememberSaveable(servicio.id) { mutableStateOf(servicio.nombre) }
        var descripcion by rememberSaveable(servicio.id) { mutableStateOf(servicio.descripcion.orEmpty()) }
        var categoria by rememberSaveable(servicio.id) { mutableStateOf(servicio.categoria.orEmpty()) }
        var imagen by rememberSaveable(servicio.id) { mutableStateOf(servicio.imagen.orEmpty()) }
        var destacado by rememberSaveable(servicio.id) { mutableStateOf(servicio.destacado) }
        var activo by rememberSaveable(servicio.id) { mutableStateOf(servicio.activo) }
        var precio by rememberSaveable(servicio.id) { mutableStateOf(servicio.precio.toPlainString()) }
        var duracion by rememberSaveable(servicio.id) { mutableStateOf(servicio.duracion.orEmpty()) }
        var detalle by rememberSaveable(servicio.id) { mutableStateOf(servicio.descripcionDetalle.orEmpty()) }
        var horario by rememberSaveable(servicio.id) { mutableStateOf(servicio.horario.orEmpty()) }
        var incluidos by rememberSaveable(servicio.id) { mutableStateOf(servicio.incluidos.orEmpty().joinToString("\n")) }
        var etiquetas by rememberSaveable(servicio.id) { mutableStateOf(servicio.etiquetas.orEmpty().joinToString("\n")) }

        Field("Nombre", nombre) { nombre = it }
        Field("Descripción", descripcion, multilinea = true) { descripcion = it }
        Field("Categoría", categoria) { categoria = it }
        Field("Ruta o URL de imagen", imagen) { imagen = it }
        Field("Precio", precio, tipo = KeyboardType.Decimal) { precio = it }
        Field("Duración", duracion) { duracion = it }
        Field("Descripción ampliada", detalle, multilinea = true) { detalle = it }
        Field("Horario", horario) { horario = it }
        Field("Incluidos (uno por línea)", incluidos, multilinea = true) { incluidos = it }
        Field("Etiquetas (una por línea)", etiquetas, multilinea = true) { etiquetas = it }
        Selector("Destacado", destacado, listOf(true, false), { if (it) "Sí" else "No" }) { destacado = it }
        Selector("Activo", activo, listOf(true, false), { if (it) "Sí" else "No" }) { activo = it }
        val importe = precio.trim().replace(',', '.').toBigDecimalOrNull()
        Primary("Guardar servicio", nombre.isNotBlank() && categoria.isNotBlank() && importe != null &&
            importe.signum() >= 0 && !estado.guardando) {
            val valor = importe ?: return@Primary
            val solicitud = Servicio(id, nombre.trim(), descripcion.trim(), categoria.trim(), imagen.trim(),
                destacado, valor, activo, duracion.trim(), detalle.trim(), horario.trim(),
                incluidos.lines().map(String::trim).filter(String::isNotBlank),
                etiquetas.lines().map(String::trim).filter(String::isNotBlank))
            model.ejecutar("Servicio guardado", recargar = false) {
                admin.guardar(solicitud)
                volver()
            }
        }
    }
}

@Composable
fun AdminOperadoresScreen(admin: AdminRepository) {
    val model = screenModel("admin-operadores", admin::operadores)
    var nombre by rememberSaveable { mutableStateOf("") }
    var borrar by remember { mutableStateOf<Operador?>(null) }
    RemotePage("Operadores", model) { operadores, estado ->
        Field("Nombre del nuevo operador", nombre) { if (it.length <= 100) nombre = it }
        Primary("Crear operador", nombre.isNotBlank() && !estado.guardando) {
            val valor = nombre.trim()
            model.ejecutar("Operador creado") {
                admin.crearOperador(OperadorSolicitud(valor))
                nombre = ""
            }
        }
        if (operadores.isEmpty()) Text("No hay operadores registrados.")
        operadores.forEach { operador ->
            Panel {
                Section(operador.nombre)
                Text(if (operador.activo) "Activo" else "Inactivo")
                Secondary(if (operador.activo) "Desactivar" else "Activar", !estado.guardando) {
                    model.ejecutar("Estado actualizado") { admin.cambiarOperador(operador.id) }
                }
                TextButton(onClick = { borrar = operador }, enabled = !estado.guardando) { Text("Eliminar") }
            }
        }
    }
    borrar?.let { operador ->
        Confirmar("Eliminar operador", operador.nombre, "Eliminar", cerrar = { borrar = null }) {
            model.ejecutar("Operador eliminado") { admin.borrarOperador(operador.id) }
        }
    }
}
