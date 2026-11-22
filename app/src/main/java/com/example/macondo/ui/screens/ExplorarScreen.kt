package com.example.macondo.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.macondo.ui.components.*
import com.example.macondo.ui.panorama.PanoramaView

private data class Escena(val nombre: String, val archivo: String, val x: Int, val y: Int, val vecinos: List<String>)

private val escenas = linkedMapOf(
    "exterior" to Escena("Exterior", "escenario/exterior-360", 1, 0, listOf("lobby", "playa")),
    "lobby" to Escena("Lobby", "escenario/lobby-360", 1, 2, listOf("exterior", "pasillo", "piscina", "restaurante")),
    "pasillo" to Escena("Pasillo", "escenario/pasillo-360", 1, 4, listOf("lobby", "habitacion", "estandar", "superior", "ejecutiva", "suite", "vip")),
    "restaurante" to Escena("Restaurante", "escenario/restaurante-360", 0, 2, listOf("lobby")),
    "piscina" to Escena("Piscina", "escenario/piscina-360", 2, 2, listOf("lobby", "playa")),
    "playa" to Escena("Playa", "escenario/playa-360", 2, 0, listOf("exterior", "piscina")),
    "habitacion" to Escena("Habitación", "habitacion-blender-360", 1, 5, listOf("pasillo")),
    "estandar" to Escena("Estándar", "habitacion-estandar-360", 0, 3, listOf("pasillo")),
    "superior" to Escena("Superior", "habitacion-superior-360", 2, 3, listOf("pasillo")),
    "ejecutiva" to Escena("Ejecutiva", "habitacion-ejecutiva-360", 0, 4, listOf("pasillo")),
    "suite" to Escena("Suite", "habitacion-suite-360", 2, 4, listOf("pasillo")),
    "vip" to Escena("Suite VIP", "habitacion-vip-360", 2, 5, listOf("pasillo"))
)

@Composable
fun ExplorarScreen() {
    var actual by rememberSaveable { mutableStateOf("exterior") }
    var destino by rememberSaveable { mutableStateOf<String?>(null) }
    val escena = escenas.getValue(actual)
    val camino = destino?.let { recorrido(actual, it) }.orEmpty()

    Page("Explorar el hotel") {
        Section(escena.nombre)
        key(actual) {
            AndroidView(
                factory = { context -> PanoramaView(context, "images/${escena.archivo}.jpg").apply {
                    contentDescription = "Vista panorámica de ${escena.nombre}"
                } },
                modifier = Modifier.fillMaxWidth().aspectRatio(1.45f),
                update = {},
                onRelease = { it.onPause() }
            )
        }
        Text("Desliza sobre la imagen para mirar alrededor.")
        Selector("Destino", destino, escenas.keys.toList(), { escenas.getValue(it).nombre }) { destino = it }
        val siguiente = camino.getOrNull(1)
        if (siguiente != null) {
            Text(camino.joinToString(" → ") { escenas.getValue(it).nombre })
            Primary("Continuar a ${escenas.getValue(siguiente).nombre}") { actual = siguiente }
        }
        val activo = MaterialTheme.colorScheme.primary
        val colorCamino = MaterialTheme.colorScheme.secondary
        val neutro = MaterialTheme.colorScheme.outlineVariant
        Canvas(Modifier.fillMaxWidth().height(128.dp)) {
            fun punto(lugar: Escena) = Offset(size.width * (.15f + .35f * lugar.x), size.height * (.12f + .15f * lugar.y))
            escenas.values.forEach { lugar ->
                lugar.vecinos.forEach { vecino -> drawLine(neutro, punto(lugar), punto(escenas.getValue(vecino)), 2.dp.toPx()) }
            }
            camino.zipWithNext().forEach { (desde, hasta) ->
                drawLine(colorCamino, punto(escenas.getValue(desde)), punto(escenas.getValue(hasta)), 3.dp.toPx())
            }
            escenas.forEach { (id, lugar) ->
                drawCircle(if (id == actual) activo else neutro, if (id == actual) 7.dp.toPx() else 4.dp.toPx(), punto(lugar))
            }
        }
        Section("Lugares conectados")
        escena.vecinos.forEach { vecino ->
            Secondary(escenas.getValue(vecino).nombre) { actual = vecino }
        }
    }
}

private fun recorrido(origen: String, destino: String): List<String> {
    val pendientes = ArrayDeque<List<String>>()
    val visitados = mutableSetOf(origen)
    pendientes.add(listOf(origen))
    while (pendientes.isNotEmpty()) {
        val camino = pendientes.removeFirst()
        val ultimo = camino.last()
        if (ultimo == destino) return camino
        escenas.getValue(ultimo).vecinos.filter(visitados::add).forEach { pendientes.add(camino + it) }
    }
    return emptyList()
}
