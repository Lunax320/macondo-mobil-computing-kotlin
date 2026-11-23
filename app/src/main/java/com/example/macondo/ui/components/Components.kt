package com.example.macondo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.macondo.R
import com.example.macondo.data.TipoHabitacion
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.macondo.ui.ScreenModel
import com.example.macondo.ui.ScreenState
import com.example.macondo.ui.theme.AzulProfundoMacondo
import com.example.macondo.ui.theme.DoradoMacondo
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun Page(
    titulo: String,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (titulo.isNotBlank()) Text(titulo, style = MaterialTheme.typography.headlineMedium)
            contenido()
        }
    }
}

@Composable
fun <T> RemotePage(
    titulo: String,
    model: ScreenModel<T>,
    pie: @Composable ColumnScope.(ScreenState<T>) -> Unit = {},
    contenido: @Composable ColumnScope.(T, ScreenState<T>) -> Unit
) {
    val state by model.estado.collectAsStateWithLifecycle()
    Page(titulo) {
        if (state.cargando || state.guardando) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            if (!state.guardando) TextButton(onClick = model::recargar) { Text("Reintentar") }
        }
        state.aviso?.let { mensaje ->
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(mensaje, Modifier.weight(1f))
                    TextButton(onClick = model::avisoMostrado) { Text("Cerrar") }
                }
            }
        }
        state.datos?.let { contenido(it, state) }
        pie(state)
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = contenido
        )
    }
}

@Composable
fun TipoHabitacionOption(tipo: TipoHabitacion, selected: Boolean, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HotelImage(tipo.imagen, tipo.nombre, Modifier.width(88.dp).height(82.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tipo.nombre, style = MaterialTheme.typography.titleMedium)
                Text("Hasta ${tipo.capacidadPersonas} huéspedes", style = MaterialTheme.typography.bodyMedium)
                Text("${moneda(tipo.precioNoche)} por noche", style = MaterialTheme.typography.labelLarge)
            }
            if (selected) Icon(
                painterResource(R.drawable.ic_check), "Habitación seleccionada",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun FormSection(titulo: String, ayuda: String? = null, contenido: @Composable ColumnScope.() -> Unit) {
    Panel {
        Section(titulo)
        ayuda?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        contenido()
    }
}

@Composable
fun Section(titulo: String) = Text(titulo, style = MaterialTheme.typography.titleLarge)

@Composable
fun Primary(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(label)
    }
}

@Composable
fun Secondary(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(label)
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    secret: Boolean = false,
    tipo: KeyboardType = KeyboardType.Text,
    multilinea: Boolean = false,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
    onChange: (String) -> Unit
) {
    var mostrarClave by rememberSaveable(label) { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = !multilinea,
        readOnly = readOnly,
        isError = isError,
        supportingText = supportingText?.let { texto -> { Text(texto) } },
        trailingIcon = if (secret) {
            { TextButton(onClick = { mostrarClave = !mostrarClave }) { Text(if (mostrarClave) "Ocultar" else "Ver") } }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else tipo),
        visualTransformation = if (secret && !mostrarClave) PasswordVisualTransformation() else VisualTransformation.None
    )
}

@Composable
fun <T> ChoiceChips(
    label: String,
    actual: T,
    opciones: List<T>,
    texto: (T) -> String,
    seleccionar: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            opciones.forEach { opcion ->
                FilterChip(
                    selected = actual == opcion,
                    onClick = { seleccionar(opcion) },
                    label = { Text(texto(opcion)) }
                )
            }
        }
    }
}

@Composable
fun SliderField(
    label: String,
    value: Int,
    range: IntRange,
    unit: String = "",
    onChange: (Int) -> Unit
) {
    require(range.first < range.last) { "El slider necesita un rango de al menos dos valores." }
    val valorSeguro = value.coerceIn(range)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            Text(valorConUnidad(valorSeguro, unit), style = MaterialTheme.typography.titleMedium)
        }
        Slider(
            value = valorSeguro.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(range)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(valorConUnidad(range.first, unit), style = MaterialTheme.typography.labelSmall)
            Text(valorConUnidad(range.last, unit), style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun valorConUnidad(valor: Int, unidad: String): String {
    val unidadVisible = if (valor == 1) when (unidad) {
        "personas" -> "persona"
        "huéspedes" -> "huésped"
        else -> unidad
    } else unidad
    return listOf(valor.toString(), unidadVisible).filter(String::isNotBlank).joinToString(" ")
}

@Composable
fun StepperField(label: String, value: Int, minimo: Int = 1, maximo: Int = Int.MAX_VALUE, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onChange((value - 1).coerceAtLeast(minimo)) }, enabled = value > minimo) {
                Text("−", style = MaterialTheme.typography.titleLarge)
            }
            Text(value.toString(), style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { onChange(if (value >= maximo) maximo else value + 1) }, enabled = value < maximo) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
fun SwitchField(label: String, checked: Boolean, onChange: (Boolean) -> Unit, ayuda: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            ayuda?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun TagEditor(label: String, items: List<String>, onItemsChange: (List<String>) -> Unit) {
    var nuevo by rememberSaveable(label) { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        if (items.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    InputChip(
                        selected = true,
                        onClick = { onItemsChange(items.filterNot { it == item }) },
                        label = { Text(item) },
                        trailingIcon = { Icon(painterResource(R.drawable.ic_close), "Quitar $item", Modifier.size(16.dp)) }
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = nuevo,
                onValueChange = { nuevo = it },
                modifier = Modifier.weight(1f),
                label = { Text("Añadir elemento") },
                singleLine = true
            )
            TextButton(
                enabled = nuevo.isNotBlank() && items.none { it.equals(nuevo.trim(), ignoreCase = true) },
                onClick = {
                    onItemsChange(items + nuevo.trim())
                    nuevo = ""
                }
            ) { Text("Añadir") }
        }
    }
}

@Composable
fun <T> Selector(
    label: String,
    actual: T?,
    items: List<T>,
    texto: (T) -> String,
    modifier: Modifier = Modifier,
    seleccionar: (T) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { abierto = true }, enabled = items.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(actual?.let(texto) ?: "Seleccionar", Modifier.weight(1f))
                    Icon(painterResource(R.drawable.ic_dropdown), contentDescription = null)
                }
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                items.forEach { item ->
                    DropdownMenuItem(text = { Text(texto(item)) }, onClick = {
                        abierto = false
                        seleccionar(item)
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, fecha: String, modifier: Modifier = Modifier, actualizar: (String) -> Unit) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Secondary(if (fecha.isBlank()) "Elegir fecha" else fechaVisible(fecha)) { abierto = true }
    }
    if (abierto) {
        val millis = runCatching { LocalDate.parse(fecha).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }
            .getOrNull()
        val picker = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(enabled = picker.selectedDateMillis != null, onClick = {
                    picker.selectedDateMillis?.let { actualizar(LocalDate.ofEpochDay(Math.floorDiv(it, 86_400_000L)).toString()) }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cerrar") } }
        ) {
            DatePicker(state = picker, showModeToggle = false)
        }
    }
}

@Composable
fun Confirmar(
    titulo: String,
    texto: String,
    confirmar: String,
    destructiva: Boolean = false,
    cerrar: () -> Unit,
    accion: () -> Unit
) {
    AlertDialog(
        onDismissRequest = cerrar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(
                onClick = { cerrar(); accion() },
                colors = ButtonDefaults.textButtonColors(contentColor = if (destructiva) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary)
            ) { Text(confirmar) }
        },
        dismissButton = { TextButton(onClick = cerrar) { Text("Cancelar") } }
    )
}

@Composable
fun HotelImage(ruta: String?, descripcion: String?, modifier: Modifier = Modifier) {
    if (ruta.isNullOrBlank()) return
    val contexto = LocalContext.current
    // La web identifica estas imágenes como AVIF; el APK incluye JPEG para admitir API 24.
    val recurso = if (ruta.endsWith(".avif", ignoreCase = true)) ruta.dropLast(5) + ".jpg" else ruta
    val fuente = when {
        ruta.startsWith("https://") -> ruta
        ruta.startsWith("/images/") -> "file:///android_asset/" + recurso.removePrefix("/")
        ruta.startsWith("images/") -> "file:///android_asset/$recurso"
        else -> null
    } ?: return
    AsyncImage(
        model = ImageRequest.Builder(contexto).data(fuente).crossfade(true).build(),
        contentDescription = descripcion,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
fun Hero() {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 252.dp).clip(MaterialTheme.shapes.extraLarge)
            .background(AzulProfundoMacondo)
    ) {
        HotelImage("/images/Playa.avif", null, Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(
            Brush.verticalGradient(listOf(AzulProfundoMacondo.copy(alpha = 0.08f), AzulProfundoMacondo.copy(alpha = 0.92f)))
        ))
        Column(
            Modifier.align(Alignment.BottomStart).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Cartagena de Indias", color = DoradoMacondo, style = MaterialTheme.typography.labelLarge)
            Text("Hotel Macondo", color = Color.White, style = MaterialTheme.typography.displaySmall)
            Text("Donde el realismo mágico encuentra el mar", color = Color.White)
        }
    }
}

fun moneda(valor: BigDecimal?): String = valor?.let {
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).format(it)
} ?: "Sin precio"

private val formatoFecha = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-CO"))

internal fun fechaVisible(fecha: String): String = runCatching {
    LocalDate.parse(fecha).format(formatoFecha)
}.getOrDefault(fecha)
