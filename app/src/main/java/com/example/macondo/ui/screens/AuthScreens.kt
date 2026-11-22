package com.example.macondo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import com.example.macondo.data.*
import com.example.macondo.data.repository.AccountRepository
import com.example.macondo.ui.components.*
import com.example.macondo.ui.screenModel

@Composable
fun LoginScreen(cuentas: AccountRepository, registrar: () -> Unit) {
    val model = screenModel("login") { Unit }
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }

    RemotePage("Iniciar sesión", model) { _, state ->
        Field("Correo electrónico", correo, tipo = KeyboardType.Email) { correo = it }
        Field("Contraseña", clave, secret = true) { clave = it }
        Primary("Entrar", correo.isNotBlank() && clave.isNotEmpty() && !state.guardando) {
            model.ejecutar("Sesión iniciada", recargar = false) {
                cuentas.iniciar(LoginSolicitud(correo.trim(), clave))
                clave = ""
            }
        }
        Secondary("Crear una cuenta", enabled = !state.guardando, onClick = registrar)
    }
}

@Composable
fun RegistroScreen(cuentas: AccountRepository, entrar: () -> Unit) {
    val model = screenModel("registro") { Unit }
    var clave by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }

    RemotePage("Crear cuenta", model) { _, state ->
        ClienteFields(null) { cliente, valido ->
            Field("Contraseña", clave, secret = true) { clave = it }
            Field("Confirmar contraseña", confirmacion, secret = true) { confirmacion = it }
            if (confirmacion.isNotEmpty() && clave != confirmacion) Text("Las contraseñas deben coincidir.")
            Primary("Registrarme", valido && clave.isNotEmpty() && confirmacion == clave && !state.guardando) {
                model.ejecutar("Cuenta creada", recargar = false) {
                    cuentas.registro(RegistroSolicitud(cliente, clave))
                    clave = ""
                    entrar()
                }
            }
        }
    }
}

@Composable
fun ClienteFields(
    inicial: Cliente?,
    contenido: @Composable (Cliente, Boolean) -> Unit
) {
    var nombre by rememberSaveable(inicial?.nombre) { mutableStateOf(inicial?.nombre.orEmpty()) }
    var apellido by rememberSaveable(inicial?.apellido) { mutableStateOf(inicial?.apellido.orEmpty()) }
    var telefono by rememberSaveable(inicial?.telefono) { mutableStateOf(inicial?.telefono.orEmpty()) }
    var cedula by rememberSaveable(inicial?.cedula) { mutableStateOf(inicial?.cedula.orEmpty()) }
    var correo by rememberSaveable(inicial?.correo) { mutableStateOf(inicial?.correo.orEmpty()) }
    Field("Nombre", nombre) { nombre = it }
    Field("Apellido", apellido) { apellido = it }
    Field("Cédula", cedula, tipo = KeyboardType.Number) { cedula = it }
    Field("Teléfono", telefono, tipo = KeyboardType.Phone) { telefono = it }
    Field("Correo electrónico", correo, tipo = KeyboardType.Email) { correo = it }
    val datos = Cliente(inicial?.id, nombre.trim(), apellido.trim(), cedula.trim(), telefono.trim(), correo.trim())
    val valido = listOf(nombre, apellido, cedula, telefono).all { it.isNotBlank() } &&
        android.util.Patterns.EMAIL_ADDRESS.matcher(datos.correo).matches()
    contenido(datos, valido)
}
