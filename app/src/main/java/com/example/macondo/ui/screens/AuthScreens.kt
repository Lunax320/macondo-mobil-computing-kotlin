package com.example.macondo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import com.example.macondo.data.*
import com.example.macondo.data.repository.AccountRepository
import com.example.macondo.ui.components.*
import com.example.macondo.ui.screenModel

@Composable
fun LoginScreen(
    cuentas: AccountRepository, verificacionCorreo: Boolean,
    recuperar: (() -> Unit)?, registrar: () -> Unit
) {
    val model = screenModel("login") { Unit }
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }

    RemotePage("Iniciar sesión", model) { _, state ->
        FormSection("Tus datos de acceso", "Usa el correo y la contraseña asociados a tu cuenta.") {
            val correoInvalido = correo.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()
            Field("Correo electrónico", correo, tipo = KeyboardType.Email, isError = correoInvalido,
                supportingText = if (correoInvalido) "Revisa el formato del correo." else null) { correo = it }
            Field("Contraseña", clave, secret = true) { clave = it }
        }
        Primary("Entrar", correoValido(correo) && clave.isNotEmpty() && !state.guardando) {
            model.ejecutar("Sesión iniciada", recargar = false) {
                cuentas.iniciar(LoginSolicitud(correo.trim(), clave))
                clave = ""
            }
        }
        recuperar?.let { TextButton(onClick = it, enabled = !state.guardando) { Text("Olvidé mi contraseña") } }
        if (verificacionCorreo && correoValido(correo, soloGmail = true)) {
            TextButton(enabled = !state.guardando, onClick = {
                val solicitud = CorreoSolicitud(correo.trim())
                model.ejecutar("Si tu cuenta necesita verificación, revisa tu correo.", recargar = false) {
                    cuentas.reenviarVerificacion(solicitud)
                }
            }) { Text("Reenviar correo de verificación") }
        }
        Secondary("Crear una cuenta", enabled = !state.guardando, onClick = registrar)
    }
}

@Composable
fun RegistroScreen(cuentas: AccountRepository, verificacionCorreo: Boolean, entrar: (String) -> Unit) {
    val model = screenModel("registro") { Unit }
    var clave by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }

    RemotePage("Crear cuenta", model) { _, state ->
        ClienteFields(null, soloGmail = verificacionCorreo) { cliente, valido ->
            FormSection("Seguridad", "Crea una contraseña y confírmala para proteger tu cuenta.") {
                Field("Contraseña", clave, secret = true) { clave = it }
                val noCoinciden = confirmacion.isNotEmpty() && clave != confirmacion
                Field("Confirmar contraseña", confirmacion, secret = true, isError = noCoinciden,
                    supportingText = if (noCoinciden) "Las contraseñas no coinciden." else null) { confirmacion = it }
            }
            Primary("Registrarme", valido && clave.isNotEmpty() && confirmacion == clave && !state.guardando) {
                model.ejecutar("Cuenta creada", recargar = false) {
                    val cuenta = cuentas.registro(RegistroSolicitud(cliente, clave))
                    clave = ""
                    entrar(cuenta.mensaje ?: if (verificacionCorreo) "Revisa tu correo para activar la cuenta."
                        else "Cuenta creada. Ya puedes iniciar sesión.")
                }
            }
        }
    }
}

@Composable
fun RecuperarContrasenaScreen(cuentas: AccountRepository) {
    val model = screenModel("recuperar-contrasena") { Unit }
    var correo by rememberSaveable { mutableStateOf("") }
    var mensaje by rememberSaveable { mutableStateOf<String?>(null) }
    RemotePage("Recuperar contraseña", model) { _, estado ->
        if (mensaje == null) {
            val valido = correoValido(correo, soloGmail = true)
            Field("Correo de tu cuenta", correo, tipo = KeyboardType.Email,
                isError = correo.isNotBlank() && !valido, supportingText = "Usa tu dirección de Gmail.") { correo = it }
            Primary("Enviar enlace", valido && !estado.guardando) {
                val solicitud = CorreoSolicitud(correo.trim())
                model.ejecutar("Solicitud enviada", recargar = false) {
                    mensaje = cuentas.solicitarRecuperacion(solicitud).mensaje
                }
            }
        } else {
            Section("Revisa tu correo")
            Text(mensaje.orEmpty())
            Text("Abre el enlace recibido para elegir tu nueva contraseña.")
        }
    }
}

@Composable
fun ClienteFields(
    inicial: Cliente?,
    soloGmail: Boolean = false,
    contenido: @Composable (Cliente, Boolean) -> Unit
) {
    var nombre by rememberSaveable(inicial?.nombre) { mutableStateOf(inicial?.nombre.orEmpty()) }
    var apellido by rememberSaveable(inicial?.apellido) { mutableStateOf(inicial?.apellido.orEmpty()) }
    var telefono by rememberSaveable(inicial?.telefono) { mutableStateOf(inicial?.telefono.orEmpty()) }
    var cedula by rememberSaveable(inicial?.cedula) { mutableStateOf(inicial?.cedula.orEmpty()) }
    var correo by rememberSaveable(inicial?.correo) { mutableStateOf(inicial?.correo.orEmpty()) }
    FormSection("Información personal", "Estos datos identifican al titular de la reserva.") {
        Field("Nombre", nombre, isError = nombre.isNotBlank() && nombre.trim().length < 2,
            supportingText = if (nombre.isNotBlank() && nombre.trim().length < 2) "Escribe al menos dos caracteres." else null) { nombre = it }
        Field("Apellido", apellido, isError = apellido.isNotBlank() && apellido.trim().length < 2,
            supportingText = if (apellido.isNotBlank() && apellido.trim().length < 2) "Escribe al menos dos caracteres." else null) { apellido = it }
        Field("Cédula", cedula, tipo = KeyboardType.Number, readOnly = inicial != null) { cedula = it }
    }
    FormSection("Contacto") {
        Field("Teléfono", telefono, tipo = KeyboardType.Phone) { telefono = it }
        val correoInvalido = correo.isNotBlank() && !correoValido(correo, soloGmail)
        Field("Correo electrónico", correo, tipo = KeyboardType.Email, isError = correoInvalido,
            supportingText = when {
                soloGmail -> "Usa una dirección de Gmail."
                correoInvalido -> "Revisa el formato del correo."
                else -> null
            }) { correo = it }
    }
    val datos = Cliente(inicial?.id, nombre.trim(), apellido.trim(), cedula.trim(), telefono.trim(), correo.trim())
    val valido = listOf(nombre, apellido, cedula, telefono).all { it.isNotBlank() } &&
        correoValido(datos.correo, soloGmail)
    contenido(datos, valido)
}

private fun correoValido(correo: String, soloGmail: Boolean = false): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches() &&
        (!soloGmail || correo.trim().substringAfterLast('@').equals("gmail.com", ignoreCase = true))
