package com.example.macondo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.macondo.R
import java.math.BigDecimal

@Composable
fun DescuentoReserva(codigo: String?, descuento: BigDecimal?) {
    if (descuento == null || descuento.signum() <= 0) return

    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(painterResource(R.drawable.ic_offer), null, Modifier.size(22.dp))
            Column {
                Text(
                    codigo?.takeIf { it.isNotBlank() }?.let { "Cupón $it aplicado" } ?: "Descuento aplicado",
                    style = MaterialTheme.typography.labelLarge
                )
                Text("Ahorraste ${moneda(descuento)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
