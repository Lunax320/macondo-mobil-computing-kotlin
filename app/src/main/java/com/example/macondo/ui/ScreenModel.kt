package com.example.macondo.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.macondo.data.remote.mensajeParaUsuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScreenState<T>(
    val datos: T? = null,
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val error: String? = null,
    val aviso: String? = null
)

class ScreenModel<T>(private val cargarDatos: suspend () -> T) : ViewModel() {
    private val estadoMutable = MutableStateFlow(ScreenState<T>())
    val estado = estadoMutable.asStateFlow()
    private var consulta: Job? = null

    init {
        recargar()
    }

    fun recargar() {
        if (estado.value.guardando) return
        consulta?.cancel()
        consulta = viewModelScope.launch {
            estadoMutable.update { it.copy(cargando = true, error = null) }
            try {
                val datos = cargarDatos()
                estadoMutable.update { it.copy(datos = datos, cargando = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                estadoMutable.update { it.copy(cargando = false, error = error.mensajeParaUsuario()) }
            }
        }
    }

    fun ejecutar(
        aviso: String,
        recargar: Boolean = true,
        operacion: suspend () -> Unit
    ) {
        if (estado.value.guardando || estado.value.cargando) return
        estadoMutable.update { it.copy(guardando = true, error = null, aviso = null) }
        viewModelScope.launch {
            try {
                operacion()
                estadoMutable.update { it.copy(guardando = false, aviso = aviso) }
                if (recargar) recargar()
            } catch (error: CancellationException) {
                estadoMutable.update { it.copy(guardando = false) }
                throw error
            } catch (error: Exception) {
                estadoMutable.update { it.copy(guardando = false, error = error.mensajeParaUsuario()) }
            }
        }
    }

    fun avisoMostrado() = estadoMutable.update { it.copy(aviso = null) }
}

@Composable
fun <T> screenModel(key: String, cargar: suspend () -> T): ScreenModel<T> =
    viewModel(key = key, factory = viewModelFactory { initializer { ScreenModel(cargar) } })
