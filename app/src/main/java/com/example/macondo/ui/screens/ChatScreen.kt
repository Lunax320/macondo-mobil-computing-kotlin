package com.example.macondo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.macondo.R
import com.example.macondo.data.Sesion
import com.example.macondo.ui.chat.ChatViewModel
import com.example.macondo.ui.components.Confirmar
import com.example.macondo.ui.navigation.destino

private val sugerenciasChat = listOf(
    "¿Cómo puedo reservar?",
    "¿Qué servicios ofrece el hotel?",
    "¿Cómo puedo cancelar mi reserva?"
)

@Composable
internal fun ChatScreen(
    model: ChatViewModel,
    sesion: Sesion?,
    navegar: (String) -> Unit
) {
    val estado by model.estado.collectAsStateWithLifecycle()
    val lista = rememberLazyListState()
    val enfoque = LocalFocusManager.current
    var confirmarLimpieza by remember { mutableStateOf(false) }
    val conversacionIniciada = estado.turnos.isNotEmpty() || estado.pendiente != null
    val puedeEnviar = estado.borrador.isNotBlank() && !estado.enviando
    val enviar = {
        if (puedeEnviar) {
            enfoque.clearFocus()
            model.enviar()
        }
    }

    LaunchedEffect(estado.turnos.lastOrNull(), estado.pendiente, estado.error) {
        val ultimo = estado.turnos.size + if (estado.pendiente != null) 1 else 0
        lista.scrollToItem(ultimo)
    }

    Column(
        Modifier.fillMaxSize().imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.widthIn(max = 680.dp).fillMaxWidth().weight(1f)) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_chat), contentDescription = null)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Asistente Macondo",
                            Modifier.semantics { heading() },
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Ayuda con tu estadía",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (conversacionIniciada) {
                        IconButton(onClick = { confirmarLimpieza = true }) {
                            Icon(painterResource(R.drawable.ic_new_chat), "Nueva conversación")
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                state = lista,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    BurbujaChat("¡Hola! Puedo ayudarte con habitaciones, servicios y reservas. ¿Qué quieres consultar?")
                    if (!conversacionIniciada) {
                        Spacer(Modifier.height(12.dp))
                        sugerenciasChat.forEach { sugerencia ->
                            OutlinedButton(
                                onClick = {
                                    enfoque.clearFocus()
                                    model.enviar(sugerencia)
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) { Text(sugerencia) }
                        }
                    }
                }
                items(estado.turnos) { turno ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BurbujaChat(turno.pregunta, delHuesped = true)
                        BurbujaChat(turno.respuesta, anunciar = turno === estado.turnos.lastOrNull())
                        turno.acciones.mapNotNull { it.destino(sesion) }.distinctBy { it.ruta }
                            .forEach { accion ->
                                FilledTonalButton(
                                    onClick = { navegar(accion.ruta) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) { Text(accion.etiqueta) }
                            }
                    }
                }
                estado.pendiente?.let { pregunta ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            BurbujaChat(pregunta, delHuesped = true)
                            val error = estado.error
                            if (error != null) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ) {
                                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                        Text(error, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                                        TextButton(onClick = model::reintentar) { Text("Reintentar") }
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text(
                                        "El asistente está preparando una respuesta",
                                        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = estado.borrador,
                            onValueChange = model::editar,
                            modifier = Modifier.weight(1f),
                            label = { Text("Mensaje") },
                            maxLines = 4,
                            shape = MaterialTheme.shapes.large,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { enviar() })
                        )
                        FilledIconButton(
                            onClick = enviar,
                            enabled = puedeEnviar,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(painterResource(R.drawable.ic_send), "Enviar mensaje")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Evita compartir contraseñas o datos de pago.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    if (confirmarLimpieza) {
        Confirmar(
            "Nueva conversación", "Se borrarán los mensajes de esta conversación.", "Empezar de nuevo",
            cerrar = { confirmarLimpieza = false },
            accion = model::limpiar
        )
    }
}

@Composable
private fun BurbujaChat(texto: String, delHuesped: Boolean = false, anunciar: Boolean = false) {
    val colores = MaterialTheme.colorScheme
    val esquina = 18.dp
    val contenido = remember(texto, delHuesped) {
        if (delHuesped) AnnotatedString(texto) else formatearRespuesta(texto)
    }
    Row(
        Modifier.fillMaxWidth().padding(
            start = if (delHuesped) 28.dp else 0.dp,
            end = if (delHuesped) 0.dp else 28.dp
        ),
        horizontalArrangement = if (delHuesped) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 520.dp),
            shape = RoundedCornerShape(
                topStart = esquina, topEnd = esquina,
                bottomStart = if (delHuesped) esquina else 4.dp,
                bottomEnd = if (delHuesped) 4.dp else esquina
            ),
            color = if (delHuesped) colores.primaryContainer else colores.surfaceContainerHigh,
            contentColor = if (delHuesped) colores.onPrimaryContainer else colores.onSurface
        ) {
            Text(
                contenido,
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp).then(
                    if (anunciar) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier
                ),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private val marcadoresLista = Regex("""^[ \t]*[*+\-\u2022][ \t]+""")
private val enfasis = Regex("""\*\*([^\n*]+)\*\*""")

private fun formatearRespuesta(texto: String): AnnotatedString = buildAnnotatedString {
    texto.lineSequence().forEachIndexed { indice, linea ->
        if (indice != 0) append('\n')
        val marcador = marcadoresLista.find(linea)
        if (marcador == null) {
            appendConEnfasis(linea)
        } else {
            withBulletList {
                withBulletListItem { appendConEnfasis(linea.substring(marcador.range.last + 1)) }
            }
        }
    }
}

private fun AnnotatedString.Builder.appendConEnfasis(contenido: String) {
    var posicion = 0
    enfasis.findAll(contenido).forEach { coincidencia ->
        append(contenido.substring(posicion, coincidencia.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(coincidencia.groupValues[1]) }
        posicion = coincidencia.range.last + 1
    }
    append(contenido.substring(posicion))
}
