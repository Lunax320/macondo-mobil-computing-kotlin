package com.example.macondo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.macondo.data.Reserva
import com.example.macondo.data.repository.AdminRepository
import com.example.macondo.data.repository.OperationsRepository
import com.example.macondo.ui.components.Panel
import com.example.macondo.ui.components.Section
import com.example.macondo.ui.components.fechaVisible
import com.example.macondo.ui.screenModel
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private data class OcupacionDia(val fecha: LocalDate, val activas: Int, val confirmadas: Int) {
    val total get() = activas + confirmadas
}

private data class ResumenOcupacion(
    val inventario: Int,
    val reservasActivas: Int,
    val dias: List<OcupacionDia>
) {
    val pico = dias.maxOf { it.total }
    val promedio = if (inventario == 0) 0
        else (100.0 * dias.sumOf { it.total } / (inventario * dias.size)).roundToInt()
}

private fun resumirOcupacion(inventario: Int, reservas: List<Reserva>): ResumenOcupacion {
    val hoy = LocalDate.now(ZoneId.of("America/Bogota"))
    val activas = reservas.filter { it.estado.equals("ACTIVA", ignoreCase = true) }
    val confirmadas = reservas.filter { it.estado.equals("CONFIRMADA", ignoreCase = true) }
    val dias = List(7) { indice ->
        val fecha = hoy.plusDays(indice.toLong())
        val noche = fecha.toString()
        fun habitacionesDe(lista: List<Reserva>) = lista.asSequence()
            .filter { it.fechaInicio <= noche && noche < it.fechaFin }
            .mapNotNull { it.habitacion?.id }
            .toSet()

        val ocupadas = habitacionesDe(activas)
        OcupacionDia(fecha, ocupadas.size, (habitacionesDe(confirmadas) - ocupadas).size)
    }
    return ResumenOcupacion(inventario, activas.size, dias)
}

@Composable
internal fun OcupacionAdmin(admin: AdminRepository, operaciones: OperationsRepository) {
    val modelo = screenModel("admin-ocupacion") {
        coroutineScope {
            val habitaciones = async { admin.habitaciones().size }
            val reservas = async { operaciones.reservas() }
            resumirOcupacion(habitaciones.await(), reservas.await())
        }
    }
    val estado by modelo.estado.collectAsStateWithLifecycle()

    Panel {
        Section("Ocupación de los próximos 7 días")
        if (estado.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (estado.error != null) {
            Text("No pudimos cargar la ocupación.", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = modelo::recargar) { Text("Reintentar") }
        }
        estado.datos?.let { resumen ->
            Text(
                "${resumen.reservasActivas} reservas activas · ${resumen.inventario} habitaciones",
                style = MaterialTheme.typography.bodyMedium
            )
            when {
                resumen.inventario == 0 -> Text("No hay habitaciones en el inventario.")
                resumen.pico == 0 -> Text("No hay reservas activas o confirmadas para estas noches.")
                else -> GraficoOcupacion(resumen)
            }
        }
    }
}

@Composable
private fun GraficoOcupacion(resumen: ResumenOcupacion) {
    val activas = MaterialTheme.colorScheme.primary
    val confirmadas = MaterialTheme.colorScheme.tertiary
    Text(
        "${resumen.promedio}% de ocupación media · Pico de ${resumen.pico}",
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        "${fechaVisible(resumen.dias.first().fecha.toString())} al ${fechaVisible(resumen.dias.last().fecha.toString())}",
        style = MaterialTheme.typography.bodySmall
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        resumen.dias.forEach { dia ->
            Column(
                Modifier.weight(1f).clearAndSetSemantics {
                    contentDescription = "${fechaVisible(dia.fecha.toString())}, ${dia.activas} activas y ${dia.confirmadas} confirmadas"
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(dia.total.toString(), style = MaterialTheme.typography.labelSmall)
                Box(
                    Modifier.fillMaxWidth().height(88.dp).clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (dia.total > 0) {
                        Column(
                            Modifier.fillMaxWidth().fillMaxHeight(dia.total.toFloat() / resumen.pico)
                                .clip(MaterialTheme.shapes.extraSmall)
                        ) {
                            if (dia.confirmadas > 0) {
                                Spacer(Modifier.weight(dia.confirmadas.toFloat()).fillMaxWidth().background(confirmadas))
                            }
                            if (dia.activas > 0) {
                                Spacer(Modifier.weight(dia.activas.toFloat()).fillMaxWidth().background(activas))
                            }
                        }
                    }
                }
                Text(dia.fecha.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LeyendaOcupacion(activas, "Activas")
        LeyendaOcupacion(confirmadas, "Confirmadas")
    }
}

@Composable
private fun LeyendaOcupacion(color: Color, etiqueta: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Spacer(Modifier.size(9.dp).background(color, CircleShape))
        Text(etiqueta, style = MaterialTheme.typography.labelSmall)
    }
}
