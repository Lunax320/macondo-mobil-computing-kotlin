package com.example.macondo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            Secondary("Reintentar", onClick = model::recargar)
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
    var personas by rememberSaveable { mutableIntStateOf(1) }
    var tipoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var estimarNoches by rememberSaveable { mutableStateOf(false) }
    var noches by rememberSaveable { mutableIntStateOf(1) }
    RemotePage("Habitaciones", model) { tipos, _ ->
        val maximo = (tipos.maxOfOrNull { it.capacidadPersonas } ?: 1).coerceAtLeast(1)
        val huespedes = personas.coerceIn(1, maximo)
        val tipoActivo = tipoId?.takeIf { id -> tipos.any { it.id == id } }
        val visibles = tipos.filter { it.capacidadPersonas >= huespedes && (tipoActivo == null || it.id == tipoActivo) }
        if (tipos.isNotEmpty()) {
            FiltrosHabitaciones(tipos, tipoActivo, huespedes, maximo, { tipoId = it }, { personas = it })
            if (estimarNoches) {
                StepperField("Noches", noches) { noches = it }
                TextButton(onClick = { estimarNoches = false }) { Text("Ocultar estimación") }
            } else {
                TextButton(onClick = { estimarNoches = true }) { Text("Estimar por noches") }
            }
        }
        when {
            tipos.isEmpty() -> Text("Aún no hay habitaciones en el catálogo.")
            visibles.isEmpty() -> {
                Text("Ninguna habitación coincide con los filtros.")
                TextButton(onClick = { tipoId = null; personas = 1 }) { Text("Limpiar filtros") }
            }
            else -> visibles.forEach { TipoCard(it, abrirTipo, noches.takeIf { estimarNoches }) }
        }
    }
}

@Composable
private fun FiltrosHabitaciones(
    tipos: List<TipoHabitacion>, tipoId: Long?, huespedes: Int, maximo: Int,
    elegirTipo: (Long?) -> Unit, elegirHuespedes: (Int) -> Unit
) {
    val nombres = tipos.mapNotNull { tipo -> tipo.id?.let { it to tipo.nombre } }.toMap()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Selector("Habitación", tipoId ?: 0L, listOf(0L) + nombres.keys,
            { nombres[it] ?: "Todas" }, Modifier.weight(1f)) { elegirTipo(it.takeIf { id -> id != 0L }) }
        if (maximo <= 12) {
            Selector("Huéspedes", huespedes, (1..maximo).toList(), Int::toString, Modifier.weight(1f), elegirHuespedes)
        }
    }
    if (maximo > 12) StepperField("Huéspedes", huespedes, maximo = maximo, onChange = elegirHuespedes)
}

@Composable
private fun TipoCard(tipo: TipoHabitacion, abrir: (Long) -> Unit, noches: Int? = null) {
    val id = tipo.id
    Card(onClick = { id?.let(abrir) }, enabled = id != null, modifier = Modifier.fillMaxWidth()) {
        HotelImage(tipo.imagen, null, Modifier.fillMaxWidth().height(142.dp))
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Section(tipo.nombre)
            Text("Hasta ${tipo.capacidadPersonas} huéspedes")
            Text("${moneda(tipo.precioNoche)} por noche", style = MaterialTheme.typography.titleMedium)
            if (noches != null) {
                val total = tipo.precioNoche.multiply(noches.toBigDecimal())
                Text("Estimado · ${moneda(total)} por $noches ${if (noches == 1) "noche" else "noches"}")
            }
        }
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
    var categoriaElegida by rememberSaveable { mutableStateOf<String?>(null) }
    RemotePage("Servicios", model) { servicios, _ ->
        val categorias = servicios.mapNotNull { it.categoria?.trim()?.takeIf(String::isNotEmpty) }.distinct().sorted()
        val categoriaActual = categoriaElegida?.takeIf(categorias::contains)
        if (categorias.isNotEmpty()) FiltrosServicios(categoriaActual, categorias) { categoriaElegida = it }
        val visibles = servicios.filter {
            categoriaActual == null || it.categoria?.trim() == categoriaActual
        }
        if (visibles.isEmpty()) Text("Aún no hay servicios en el catálogo.")
        visibles.forEachIndexed { index, servicio ->
            if (index > 0) HorizontalDivider()
            ServicioCard(servicio, abrir)
        }
    }
}

@Composable
private fun FiltrosServicios(actual: String?, categorias: List<String>, seleccionar: (String?) -> Unit) {
    Selector("Tipo de experiencia", actual.orEmpty(), listOf("") + categorias, { it.ifEmpty { "Todos" } }) {
        seleccionar(it.ifEmpty { null })
    }
}

@Composable
private fun ServicioCard(servicio: Servicio, abrir: (Long) -> Unit) {
    val id = servicio.id
    Surface(onClick = { id?.let(abrir) }, enabled = id != null, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            HotelImage(servicio.imagen, null, Modifier.size(88.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(servicio.nombre, style = MaterialTheme.typography.titleMedium)
                servicio.categoria?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                servicio.duracion?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(moneda(servicio.precio), style = MaterialTheme.typography.labelLarge)
            }
        }
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
