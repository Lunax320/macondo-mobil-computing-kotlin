package com.example.macondo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.macondo.ui.components.*
import com.example.macondo.ui.panorama.PanoramaView

private data class Escena(val nombre: String, val archivo: String, val vecinos: List<String>)

private val escenas = linkedMapOf(
    "exterior" to Escena("Exterior", "escenario/exterior-360", listOf("lobby", "playa")),
    "lobby" to Escena("Lobby", "escenario/lobby-360", listOf("exterior", "pasillo", "piscina", "restaurante")),
    "pasillo" to Escena("Pasillo", "escenario/pasillo-360", listOf("lobby", "habitacion", "estandar", "superior", "ejecutiva", "suite", "vip")),
    "restaurante" to Escena("Restaurante", "escenario/restaurante-360", listOf("lobby")),
    "piscina" to Escena("Piscina", "escenario/piscina-360", listOf("lobby", "playa")),
    "playa" to Escena("Playa", "escenario/playa-360", listOf("exterior", "piscina")),
    "habitacion" to Escena("Habitación", "habitacion-blender-360", listOf("pasillo")),
    "estandar" to Escena("Estándar", "habitacion-estandar-360", listOf("pasillo")),
    "superior" to Escena("Superior", "habitacion-superior-360", listOf("pasillo")),
    "ejecutiva" to Escena("Ejecutiva", "habitacion-ejecutiva-360", listOf("pasillo")),
    "suite" to Escena("Suite", "habitacion-suite-360", listOf("pasillo")),
    "vip" to Escena("Suite VIP", "habitacion-vip-360", listOf("pasillo"))
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
            Panel {
                Text("Siguiente parada", style = MaterialTheme.typography.labelLarge)
                Text(escenas.getValue(siguiente).nombre, style = MaterialTheme.typography.titleLarge)
                val restantes = camino.size - 1
                Text(if (restantes == 1) "El destino está en la siguiente parada."
                    else "Quedan $restantes paradas para llegar a ${escenas.getValue(camino.last()).nombre}.")
                Primary("Continuar recorrido") { actual = siguiente }
            }
        } else if (destino == actual) {
            Panel { Text("Llegaste a ${escena.nombre}.") }
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
