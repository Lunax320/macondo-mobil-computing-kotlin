package com.example.macondo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.macondo.data.Promocion
import com.example.macondo.data.TipoDescuento
import com.example.macondo.data.repository.PromocionesRepository
import com.example.macondo.ui.components.*
import com.example.macondo.ui.screenModel
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun AdminPromocionesScreen(repo: PromocionesRepository, abrirFormulario: (Long?) -> Unit) {
    val model = screenModel("admin-promociones", repo::todas)
    var borrar by remember { mutableStateOf<Promocion?>(null) }
    RemotePage("Promociones", model) { promociones, estado ->
        Primary("Nueva promoción", !estado.guardando && !estado.cargando) { abrirFormulario(null) }
        if (promociones.isEmpty()) Text("Aún no hay promociones.")
        promociones.forEach { promocion ->
            PromocionAdminFila(
                promocion, disponible = !estado.guardando && !estado.cargando,
                editar = { abrirFormulario(promocion.id) },
                borrar = { borrar = promocion }
            )
            HorizontalDivider()
        }
    }
    borrar?.let { promocion ->
        Confirmar("Eliminar promoción", promocion.nombre, "Eliminar", cerrar = { borrar = null }) {
            val id = promocion.id ?: return@Confirmar
            model.ejecutar("Promoción eliminada") { repo.eliminar(id) }
        }
    }
}

@Composable
private fun PromocionAdminFila(promocion: Promocion, disponible: Boolean, editar: () -> Unit, borrar: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(promocion.nombre, style = MaterialTheme.typography.titleMedium)
        Text("${promocion.codigo} · ${beneficio(promocion)}")
        Text("${fechaVisible(promocion.fechaInicio)} – ${fechaVisible(promocion.fechaFin)}")
        Text(if (promocion.activa) "Publicada" else "Pausada", style = MaterialTheme.typography.labelMedium)
        if (promocion.id != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = editar, enabled = disponible) { Text("Editar") }
            TextButton(onClick = borrar, enabled = disponible) { Text("Eliminar") }
        }
    }
}

@Composable
fun AdminPromocionFormScreen(repo: PromocionesRepository, id: Long?, alGuardar: () -> Unit) {
    val model = screenModel("admin-promocion:$id") {
        val hoy = LocalDate.now().toString()
        if (id == null) Promocion(
            codigo = "", nombre = "", descripcion = null, tipoDescuento = TipoDescuento.PORCENTAJE,
            valorDescuento = BigDecimal.TEN, fechaInicio = hoy, fechaFin = hoy, nochesMinimas = 1, activa = true
        ) else repo.todas().firstOrNull { it.id == id } ?: error("La promoción ya no está disponible.")
    }
    RemotePage(if (id == null) "Nueva promoción" else "Editar promoción", model) { inicial, estado ->
        PromocionForm(inicial, disponible = !estado.guardando && !estado.cargando) { promocion ->
            model.ejecutar("Promoción guardada", recargar = false) {
                repo.guardar(promocion)
                alGuardar()
            }
        }
    }
}

@Composable
private fun PromocionForm(inicial: Promocion, disponible: Boolean, guardar: (Promocion) -> Unit) {
    var codigo by rememberSaveable(inicial.id) { mutableStateOf(inicial.codigo) }
    var nombre by rememberSaveable(inicial.id) { mutableStateOf(inicial.nombre) }
    var descripcion by rememberSaveable(inicial.id) { mutableStateOf(inicial.descripcion.orEmpty()) }
    var tipo by rememberSaveable(inicial.id) { mutableStateOf(inicial.tipoDescuento) }
    var valor by rememberSaveable(inicial.id) { mutableStateOf(inicial.valorDescuento.toPlainString()) }
    var inicio by rememberSaveable(inicial.id) { mutableStateOf(inicial.fechaInicio) }
    var fin by rememberSaveable(inicial.id) { mutableStateOf(inicial.fechaFin) }
    var noches by rememberSaveable(inicial.id) { mutableIntStateOf(inicial.nochesMinimas) }
    var activa by rememberSaveable(inicial.id) { mutableStateOf(inicial.activa) }
    val importe = valor.trim().replace(',', '.').toBigDecimalOrNull()
    val porcentaje = tipo == TipoDescuento.PORCENTAJE
    val importeValido = importe != null && importe.signum() > 0 && (!porcentaje || importe <= BigDecimal(100))
    val fechasValidas = fin >= inicio

    Field("Nombre de la oferta", nombre) { nombre = it }
    Field("Código", codigo) { codigo = it }
    Field("Descripción (opcional)", descripcion, multilinea = true) { descripcion = it }
    HorizontalDivider()
    Section("Descuento")
    ChoiceChips("Tipo de descuento", tipo, TipoDescuento.entries,
        { if (it == TipoDescuento.PORCENTAJE) "Porcentaje" else "Valor fijo" }) { tipo = it }
    Field(if (porcentaje) "Porcentaje (%)" else "Valor en COP", valor, tipo = KeyboardType.Decimal,
        isError = !importeValido, supportingText = if (porcentaje) "Mayor que 0 y hasta 100." else "Mayor que 0.") { valor = it }
    StepperField("Noches mínimas", noches) { noches = it }
    HorizontalDivider()
    Section("Vigencia")
    DateField("Desde", inicio) { inicio = it }
    DateField("Hasta", fin) { fin = it }
    if (!fechasValidas) Text("La fecha final debe ser igual o posterior a la inicial.", color = MaterialTheme.colorScheme.error)
    SwitchField("Publicar promoción", activa, { activa = it })
    Primary("Guardar promoción", disponible && nombre.isNotBlank() && codigo.isNotBlank() && importeValido && fechasValidas) {
        val valorDescuento = importe ?: return@Primary
        guardar(inicial.copy(
            codigo = codigo.trim(), nombre = nombre.trim(), descripcion = descripcion.trim().ifEmpty { null },
            tipoDescuento = tipo, valorDescuento = valorDescuento, fechaInicio = inicio, fechaFin = fin,
            nochesMinimas = noches, activa = activa
        ))
    }
}
