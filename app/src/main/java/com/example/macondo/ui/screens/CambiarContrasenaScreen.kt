package com.example.macondo.ui.screens

import androidx.compose.runtime.*
import com.example.macondo.data.ClaveSolicitud
import com.example.macondo.data.repository.AccountRepository
import com.example.macondo.ui.components.Field
import com.example.macondo.ui.components.Primary
import com.example.macondo.ui.components.RemotePage
import com.example.macondo.ui.components.Secondary
import com.example.macondo.ui.screenModel

@Composable
fun CambiarContrasenaScreen(
    cuentas: AccountRepository,
    clienteId: Long,
    cancelar: () -> Unit,
    alGuardar: () -> Unit
) {
    val model = screenModel("cambiar-contrasena:$clienteId") { Unit }
    var actual by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }

    RemotePage("Cambiar contraseña", model) { _, estado ->
        val noCoinciden = confirmacion.isNotEmpty() && nueva != confirmacion
        Field("Contraseña actual", actual, secret = true) { actual = it }
        Field("Nueva contraseña", nueva, secret = true) { nueva = it }
        Field("Confirmar nueva contraseña", confirmacion, secret = true, isError = noCoinciden,
            supportingText = if (noCoinciden) "Las contraseñas no coinciden." else null) { confirmacion = it }
        Primary("Actualizar contraseña", actual.isNotBlank() && nueva.isNotBlank() &&
            nueva == confirmacion && !estado.guardando) {
            val solicitud = ClaveSolicitud(actual, nueva, confirmacion)
            model.ejecutar("Contraseña actualizada.", recargar = false) {
                cuentas.clave(clienteId, solicitud)
                actual = ""
                nueva = ""
                confirmacion = ""
                alGuardar()
            }
        }
        Secondary("Cancelar", enabled = !estado.guardando, onClick = cancelar)
    }
}
