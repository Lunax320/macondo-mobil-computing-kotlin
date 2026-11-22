package com.example.macondo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.macondo.BuildConfig
import com.example.macondo.ui.ScreenModel
import com.example.macondo.ui.ScreenState
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

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
    contenido: @Composable ColumnScope.(T, ScreenState<T>) -> Unit
) {
    val state by model.estado.collectAsStateWithLifecycle()
    Page(titulo) {
        if (state.cargando || state.guardando) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            if (!state.guardando) TextButton(onClick = model::recargar) { Text("Volver a consultar") }
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
    }
}

@Composable
fun Panel(contenido: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = contenido
        )
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
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = !multilinea,
        keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else tipo),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None
    )
}

@Composable
fun <T> Selector(
    label: String,
    actual: T?,
    items: List<T>,
    texto: (T) -> String,
    seleccionar: (T) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { abierto = true }, enabled = items.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(actual?.let(texto) ?: "Seleccionar")
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
fun DateField(label: String, fecha: String, actualizar: (String) -> Unit) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    Secondary(if (fecha.isBlank()) label else "$label · $fecha") { abierto = true }
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
    cerrar: () -> Unit,
    accion: () -> Unit
) {
    AlertDialog(
        onDismissRequest = cerrar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = { cerrar(); accion() }) { Text(confirmar) } },
        dismissButton = { TextButton(onClick = cerrar) { Text("Volver") } }
    )
}

@Composable
fun HotelImage(ruta: String?, descripcion: String?, modifier: Modifier = Modifier) {
    if (ruta.isNullOrBlank()) return
    val contexto = LocalContext.current
    val fuente = when {
        ruta.startsWith("https://") -> ruta
        ruta.startsWith("/images/") -> "file:///android_asset/" + ruta.removePrefix("/")
        ruta.startsWith("images/") -> "file:///android_asset/$ruta"
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
            .background(Color(0xFF183C4D))
    ) {
        HotelImage("/images/Playa.avif", null, Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(
            Brush.verticalGradient(listOf(Color(0x15152932), Color(0xEA0D2937)))
        ))
        Column(
            Modifier.align(Alignment.BottomStart).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Cartagena de Indias", color = Color(0xFFF5D394), style = MaterialTheme.typography.labelLarge)
            Text("Hotel Macondo", color = Color.White, style = MaterialTheme.typography.displaySmall)
            Text("Donde el realismo mágico encuentra el mar", color = Color.White)
        }
    }
}

fun moneda(valor: BigDecimal?): String = valor?.let {
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).format(it)
} ?: "Sin precio"
