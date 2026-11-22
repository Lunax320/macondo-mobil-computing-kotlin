package com.example.macondo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.macondo.data.Servicio
import com.example.macondo.data.TipoHabitacion
import com.example.macondo.data.repository.CatalogRepository
import com.example.macondo.ui.components.*
import com.example.macondo.ui.screenModel

@Composable
fun InicioScreen(
    catalogo: CatalogRepository,
    abrirCatalogo: () -> Unit,
    abrirTipo: (Long) -> Unit,
    abrirServicios: () -> Unit,
    abrirServicio: (Long) -> Unit,
    explorar: () -> Unit
) {
    val model = screenModel("inicio", catalogo::inicio)
    val estado by model.estado.collectAsStateWithLifecycle()
    Page("") {
        Hero()
        Primary("Ver habitaciones", onClick = abrirCatalogo)
        Secondary("Explorar el hotel", onClick = explorar)
        if (estado.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
        estado.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Secondary("Volver a consultar", onClick = model::recargar)
        }
        estado.datos?.let { inicio ->
            if (inicio.tipos.isNotEmpty()) {
                Section("Habitaciones")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(inicio.tipos, key = { it.id ?: it.nombre }) { tipo ->
                        Box(Modifier.width(256.dp)) { TipoCard(tipo, abrirTipo) }
                    }
                }
            }
            Section("Servicios")
            if (inicio.servicios.isEmpty()) Text("Por ahora no hay servicios recomendados.")
            inicio.servicios.take(3).forEach { ServicioCard(it, abrirServicio) }
            TextButton(onClick = abrirServicios) { Text("Todos los servicios") }
            if (inicio.testimonios.isNotEmpty() || !inicio.comentarios.isNullOrEmpty()) Section("Experiencias de huéspedes")
            inicio.testimonios.forEach { testimonio ->
                Panel {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HotelImage(testimonio.imagen, null, Modifier.size(52.dp))
                        Column {
                            Text(testimonio.nombre, style = MaterialTheme.typography.titleMedium)
                            testimonio.ciudad?.let { Text(it) }
                        }
                    }
                    testimonio.texto?.let { Text(it) }
                    Text("${testimonio.estrellas} de 5")
                }
            }
            inicio.comentarios.orEmpty().forEach { comentario ->
                Panel {
                    Text(comentario.nombre.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    Text(comentario.texto)
                    Text("${comentario.calificacion} de 5")
                }
            }
        }
    }
}

@Composable
fun HabitacionesScreen(catalogo: CatalogRepository, abrirTipo: (Long) -> Unit) {
    val model = screenModel("habitaciones", catalogo::tipos)
    var personas by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf("") }
    var noches by rememberSaveable { mutableStateOf("") }
    RemotePage("Habitaciones", model) { tipos, _ ->
        Field("Huéspedes", personas, tipo = KeyboardType.Number) { personas = it.filter(Char::isDigit) }
        Field("Buscar una categoría", categoria) { categoria = it }
        Field("Noches (para estimar la tarifa)", noches, tipo = KeyboardType.Number) { noches = it.filter(Char::isDigit) }
        val filtrados = tipos.filter {
            it.capacidadPersonas >= (personas.toIntOrNull() ?: 0) &&
                it.nombre.contains(categoria.trim(), ignoreCase = true)
        }
        if (filtrados.isEmpty()) Text("No hay categorías que coincidan con los filtros.")
        filtrados.forEach { TipoCard(it, abrirTipo, noches.toIntOrNull()?.takeIf { cantidad -> cantidad > 0 }) }
    }
}

@Composable
private fun TipoCard(tipo: TipoHabitacion, abrir: (Long) -> Unit, noches: Int? = null) {
    Panel {
        HotelImage(tipo.imagen, tipo.nombre, Modifier.fillMaxWidth().height(142.dp))
        Section(tipo.nombre)
        Text("${tipo.capacidadPersonas} huéspedes · ${moneda(tipo.precioNoche)} por noche")
        if (noches != null) {
            val total = tipo.precioNoche.multiply(noches.toBigDecimal())
            Text("Estimado por $noches noches · ${moneda(total)}")
        }
        tipo.id?.let { id -> TextButton(onClick = { abrir(id) }) { Text("Ver habitación") } }
    }
}

@Composable
fun DetalleHabitacionScreen(catalogo: CatalogRepository, id: Long, reservar: (Long) -> Unit) {
    val model = screenModel("tipo:$id") { catalogo.tipo(id) }
    RemotePage("Habitación", model) { tipo, _ ->
        HotelImage(tipo.imagen, tipo.nombre, Modifier.fillMaxWidth().aspectRatio(1.45f))
        Section(tipo.nombre)
        tipo.descripcion?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        Panel {
            Text("${tipo.capacidadPersonas} huéspedes")
            Text("${moneda(tipo.precioNoche)} por noche")
        }
        Primary("Consultar disponibilidad") { reservar(id) }
    }
}

@Composable
fun ServiciosScreen(catalogo: CatalogRepository, abrir: (Long) -> Unit) {
    val model = screenModel("servicios", catalogo::servicios)
    var consulta by rememberSaveable { mutableStateOf("") }
    RemotePage("Servicios", model) { servicios, _ ->
        Field("Buscar servicios", consulta) { consulta = it }
        val visibles = servicios.filter {
            it.nombre.contains(consulta.trim(), true) || it.categoria.orEmpty().contains(consulta.trim(), true)
        }
        if (visibles.isEmpty()) Text("No hay servicios que coincidan con la búsqueda.")
        visibles.forEach { ServicioCard(it, abrir) }
    }
}

@Composable
private fun ServicioCard(servicio: Servicio, abrir: (Long) -> Unit) {
    Panel {
        HotelImage(servicio.imagen, servicio.nombre, Modifier.fillMaxWidth().height(136.dp))
        Section(servicio.nombre)
        servicio.categoria?.let { Text(it) }
        servicio.descripcion?.let { Text(it) }
        servicio.id?.let { id -> TextButton(onClick = { abrir(id) }) { Text("Ver servicio") } }
    }
}

@Composable
fun DetalleServicioScreen(catalogo: CatalogRepository, id: Long, abrir: (Long) -> Unit) {
    val model = screenModel("servicio:$id") { catalogo.detalleServicio(id) }
    RemotePage("Servicio", model) { detalle, _ ->
        val servicio = detalle.servicio
        HotelImage(servicio.imagen, servicio.nombre, Modifier.fillMaxWidth().aspectRatio(1.45f))
        Section(servicio.nombre)
        (servicio.descripcionDetalle ?: servicio.descripcion)?.let { Text(it) }
        Panel {
            Text(moneda(servicio.precio))
            servicio.horario?.let { Text("Horario · $it") }
            servicio.duracion?.let { Text("Duración · $it") }
            servicio.incluidos.orEmpty().forEach { Text("• $it") }
        }
        if (detalle.relacionados.isNotEmpty()) Section("También te puede interesar")
        detalle.relacionados.forEach { ServicioCard(it, abrir) }
    }
}
