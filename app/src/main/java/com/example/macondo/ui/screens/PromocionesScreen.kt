package com.example.macondo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.macondo.R
import com.example.macondo.data.repository.PromocionesRepository
import com.example.macondo.ui.components.Panel
import com.example.macondo.ui.components.PromocionCard
import com.example.macondo.ui.screenModel

@Composable
internal fun PromocionesInicio(repo: PromocionesRepository, abrir: () -> Unit) {
    val model = screenModel("promociones-inicio", repo::publicas)
    val estado by model.estado.collectAsStateWithLifecycle()
    val destacada = estado.datos?.firstOrNull()
    if (destacada != null) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Promociones",
                modifier = Modifier.align(Alignment.CenterVertically).semantics { heading() },
                style = MaterialTheme.typography.headlineMedium
            )
            TextButton(onClick = abrir) { Text("Ver todas") }
        }
        PromocionCard(destacada, destacada = true)
    } else if (estado.error != null) {
        TextButton(onClick = abrir) { Text("Ver promociones") }
    }
}

@Composable
fun PromocionesScreen(repo: PromocionesRepository) {
    val model = screenModel("promociones", repo::publicas)
    val estado by model.estado.collectAsStateWithLifecycle()
    val ofertas = estado.datos.orEmpty()
    val anchoTarjeta = if (LocalDensity.current.fontScale > 1.3f) 360.dp else 300.dp
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(anchoTarjeta),
            modifier = Modifier.widthIn(max = 980.dp).fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Promociones", Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineMedium
                    )
                    if (ofertas.isNotEmpty()) {
                        Text(
                            "Copia un código para usarlo al reservar.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (estado.cargando) {
                item(span = { GridItemSpan(maxLineSpan) }) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            estado.error?.let { error ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Panel {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = model::recargar) { Text("Reintentar") }
                    }
                }
            }
            if (!estado.cargando && estado.error == null && ofertas.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Panel {
                        Icon(
                            painterResource(R.drawable.ic_offer), null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Sin promociones vigentes", style = MaterialTheme.typography.titleMedium)
                        Text("Las nuevas ofertas del hotel aparecerán aquí.")
                    }
                }
            }
            items(ofertas, key = { it.codigo }) { PromocionCard(it) }
        }
    }
}
