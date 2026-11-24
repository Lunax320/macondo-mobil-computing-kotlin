package com.example.macondo.ui.components

import android.content.ClipData
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.macondo.R
import com.example.macondo.data.Promocion
import com.example.macondo.data.TipoDescuento
import com.example.macondo.ui.theme.MacondoTheme
import java.math.BigDecimal
import kotlinx.coroutines.launch

private fun importePromocion(promocion: Promocion): String = when (promocion.tipoDescuento) {
    TipoDescuento.PORCENTAJE -> "${promocion.valorDescuento.stripTrailingZeros().toPlainString()} %"
    TipoDescuento.VALOR_FIJO -> moneda(promocion.valorDescuento)
}

internal fun beneficio(promocion: Promocion) = "${importePromocion(promocion)} de descuento"

@Composable
internal fun PromocionCard(promocion: Promocion, destacada: Boolean = false) {
    val colores = MaterialTheme.colorScheme
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.outlinedCardColors(
            containerColor = colores.surfaceContainerLowest,
            contentColor = colores.onSurface
        ),
        border = BorderStroke(1.dp, colores.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().background(colores.primaryContainer)) {
            if (destacada) {
                HotelImage(
                    "/images/PlayaPriv.jpg", null,
                    Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, end = 12.dp).height(116.dp)
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        importePromocion(promocion),
                        color = colores.onPrimaryContainer,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("de descuento", color = colores.onPrimaryContainer, style = MaterialTheme.typography.labelLarge)
                }
                Surface(shape = CircleShape, color = colores.secondaryContainer, contentColor = colores.onSecondaryContainer) {
                    Icon(painterResource(R.drawable.ic_offer), null, Modifier.padding(12.dp).size(24.dp))
                }
            }
        }

        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(promocion.nombre, style = MaterialTheme.typography.titleLarge)
                promocion.descripcion?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CondicionPromocion(R.drawable.ic_calendar, "Vigente hasta el ${fechaVisible(promocion.fechaFin)}")
                val unidad = if (promocion.nochesMinimas == 1) "noche" else "noches"
                CondicionPromocion(R.drawable.ic_moon, "Estadía mínima de ${promocion.nochesMinimas} $unidad")
            }
            HorizontalDivider(color = colores.outlineVariant)
            CodigoPromocion(promocion.codigo)
        }
    }
}

@Composable
private fun CondicionPromocion(@DrawableRes icono: Int, texto: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icono), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CodigoPromocion(codigo: String) {
    val clipboard = LocalClipboard.current
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    val colores = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colores.surfaceContainer,
        contentColor = colores.onSurface
    ) {
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(Modifier.widthIn(max = 168.dp).align(Alignment.CenterVertically)) {
                Text("Código promocional", style = MaterialTheme.typography.labelMedium, color = colores.onSurfaceVariant)
                Text(codigo, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium)
            }
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Código promocional", codigo)))
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            Toast.makeText(contexto, "Código copiado", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.heightIn(min = 48.dp).align(Alignment.CenterVertically),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(painterResource(R.drawable.ic_copy), null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copiar código")
            }
        }
    }
}

@Preview(name = "Oferta en tema claro", widthDp = 360, showBackground = true)
@Preview(name = "Oferta en tema oscuro con texto grande", widthDp = 320, fontScale = 1.35f,
    uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun OfertaPreview() {
    MacondoTheme {
        Box(Modifier.padding(16.dp)) {
            PromocionCard(Promocion(
                codigo = "MACONDO20", nombre = "Escapada a Cartagena",
                descripcion = "Planea un fin de semana frente al mar.",
                tipoDescuento = TipoDescuento.PORCENTAJE, valorDescuento = BigDecimal("20"),
                fechaInicio = "2026-11-01", fechaFin = "2026-12-20", nochesMinimas = 2, activa = true
            ))
        }
    }
}
