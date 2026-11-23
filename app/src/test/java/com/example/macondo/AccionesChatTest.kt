package com.example.macondo

import com.example.macondo.data.AccionChat
import com.example.macondo.data.Cliente
import com.example.macondo.data.Sesion
import com.example.macondo.ui.navigation.Routes
import com.example.macondo.ui.navigation.destino
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccionesChatTest {
    @Test
    fun accionesDeReservaNuncaAbrenPantallasDePersonalParaHuespedes() {
        val cliente = Cliente(7, "Nombre", "Apellido", "", "", "huesped@example.test")
        val huesped = Sesion(83, cliente.correo, "CLIENTE", emptyList(), cliente, "")
        val operador = Sesion(4, "operador@example.test", null, listOf("OPERADOR"), null, "")
        val administrador = operador.copy(roles = listOf("ADMINISTRADOR"))

        listOf(AccionChat.CANCELAR_RESERVA, AccionChat.CHECKOUT, AccionChat.PAGO_SEGURO).forEach { accion ->
            assertEquals("login?accion=" + accion.name, accion.destino(null)?.ruta)
            assertEquals(Routes.activas(7), accion.destino(huesped)?.ruta)
            val destinoStaff = if (accion == AccionChat.CANCELAR_RESERVA) Routes.operadorReservas else Routes.checkout()
            assertEquals(destinoStaff, accion.destino(operador)?.ruta)
            assertEquals(destinoStaff, accion.destino(administrador)?.ruta)
        }
        assertNull(AccionChat.REGISTRO.destino(huesped))
        assertNull(AccionChat.desdeId("ABRIR_URL"))
    }
}
