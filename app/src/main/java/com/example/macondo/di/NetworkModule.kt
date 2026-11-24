package com.example.macondo.di

import com.example.macondo.BuildConfig
import com.example.macondo.data.remote.*
import com.example.macondo.data.session.SessionStore
import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun gson() = Gson()

    @Provides @Singleton
    fun retrofit(gson: Gson, sesiones: SessionStore): Retrofit {
        val cliente = OkHttpClient.Builder().addInterceptor { cadena ->
            val peticion = cadena.request()
            val ruta = peticion.url.encodedPath
            if (ruta.endsWith("/chat")) {
                // El modelo del backend puede tardar hasta 120 segundos en responder.
                return@addInterceptor cadena.withReadTimeout(130, TimeUnit.SECONDS).proceed(peticion)
            }
            val esAcceso = ruta.endsWith("/v3/api-docs") || ruta.endsWith("/cliente/registro") ||
                ruta.contains("/auth/") || ruta.endsWith("/promociones/publicas") ||
                ruta.endsWith("/promociones/calcular")
            val token = if (esAcceso) null else sesiones.sesion.value?.token
            val autorizada = if (token == null) peticion
                else peticion.newBuilder().header("Authorization", "Bearer $token").build()
            cadena.proceed(autorizada).also { respuesta ->
                if (respuesta.code == 401 && token != null) sesiones.cerrar(token)
            }
        }.followRedirects(false).retryOnConnectionFailure(false).build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides fun catalogo(retrofit: Retrofit): CatalogApi = retrofit.create()
    @Provides fun cuenta(retrofit: Retrofit): AccountApi = retrofit.create()
    @Provides fun reservas(retrofit: Retrofit): ReservationApi = retrofit.create()
    @Provides fun operaciones(retrofit: Retrofit): OperationsApi = retrofit.create()
    @Provides fun admin(retrofit: Retrofit): AdminApi = retrofit.create()
    @Provides fun promociones(retrofit: Retrofit): PromocionesApi = retrofit.create()
    @Provides fun chat(retrofit: Retrofit): ChatApi = retrofit.create()
}
