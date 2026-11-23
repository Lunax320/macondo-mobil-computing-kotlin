package com.example.macondo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.macondo.R
import com.example.macondo.ui.components.Page
import com.example.macondo.ui.components.Panel
import com.example.macondo.ui.theme.Apariencia

@Composable
internal fun ConfiguracionScreen(
    apariencia: Apariencia,
    cambiarApariencia: (Apariencia) -> Unit
) {
    Page("") {
        Panel {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(painterResource(R.drawable.ic_appearance), contentDescription = null)
                Text("Tema", style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.selectableGroup()) {
                Apariencia.entries.forEach { opcion ->
                    val seleccionada = apariencia == opcion
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                            selected = seleccionada,
                            role = Role.RadioButton,
                            onClick = { cambiarApariencia(opcion) }
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(selected = seleccionada, onClick = null)
                        Text(opcion.etiqueta)
                    }
                }
            }
        }
    }
}
