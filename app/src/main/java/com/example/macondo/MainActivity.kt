package com.example.macondo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.macondo.data.repository.AccountRepository
import com.example.macondo.data.repository.CatalogRepository
import com.example.macondo.data.repository.ReservationRepository
import com.example.macondo.ui.navigation.MacondoApp
import com.example.macondo.ui.theme.MacondoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var catalogo: CatalogRepository
    @Inject lateinit var cuentas: AccountRepository
    @Inject lateinit var reservas: ReservationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MacondoTheme { MacondoApp(catalogo, cuentas, reservas) } }
    }
}
