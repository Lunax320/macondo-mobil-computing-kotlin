package com.example.macondo.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.macondo.data.Promocion
import com.example.macondo.data.TipoDescuento
import com.example.macondo.data.repository.PromocionesRepository
import com.example.macondo.ui.components.*
import com.example.macondo.ui.screenModel

@Composable
internal fun PromocionesInicio(repo: PromocionesRepository, abrir: () -> Unit) {
    val model = screenModel("promociones-inicio", repo::publicas)
    val estado by model.estado.collectAsStateWithLifecycle()
    val ofertas = estado.datos.orEmpty()
    if (ofertas.isNotEmpty()) {
        Section("Promociones")
        ofertas.take(2).forEach { PromocionCard(it) }
        TextButton(onClick = abrir) { Text("Ver todas las promociones") }
    } else if (estado.error != null) {
        TextButton(onClick = abrir) { Text("Ver promociones") }
    }
}

@Composable
fun PromocionesScreen(repo: PromocionesRepository) {
    val model = screenModel("promociones", repo::publicas)
    RemotePage("Promociones", model) { ofertas, _ ->
        if (ofertas.isEmpty()) Text("Por ahora no hay promociones vigentes.")
        ofertas.forEach { PromocionCard(it) }
    }
}

internal fun beneficio(promocion: Promocion): String = when (promocion.tipoDescuento) {
    TipoDescuento.PORCENTAJE -> "${promocion.valorDescuento.stripTrailingZeros().toPlainString()} % de descuento"
    TipoDescuento.VALOR_FIJO -> "${moneda(promocion.valorDescuento)} de descuento"
}

@Composable
private fun PromocionCard(promocion: Promocion) {
    val contexto = LocalContext.current
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(beneficio(promocion), style = MaterialTheme.typography.titleLarge)
            Text(promocion.nombre, style = MaterialTheme.typography.titleMedium)
            promocion.descripcion?.takeIf(String::isNotBlank)?.let { Text(it) }
            Text("Hasta el ${fechaVisible(promocion.fechaFin)} · Mínimo ${promocion.nochesMinimas} " +
                if (promocion.nochesMinimas == 1) "noche" else "noches")
            TextButton(onClick = {
                contexto.getSystemService(ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText("Código promocional", promocion.codigo))
            }) { Text("Copiar código ${promocion.codigo}") }
        }
    }
}
