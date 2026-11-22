package com.example.macondo.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.example.macondo.data.Sesion
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SessionStore @Inject constructor(@ApplicationContext context: Context, private val gson: Gson) {
    private val archivo = AtomicFile(context.noBackupFilesDir.resolve("sesion.macondo"))
    private val actual = MutableStateFlow<Sesion?>(null)
    val sesion = actual.asStateFlow()

    @Synchronized
    fun restaurar() {
        if (actual.value != null || !archivo.baseFile.exists()) return
        actual.value = runCatching {
            val contenido = archivo.readFully()
            val iv = contenido.copyOfRange(0, IV_BYTES)
            val cifrado = Cipher.getInstance(CIFRADO)
            cifrado.init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(128, iv))
            val json = cifrado.doFinal(contenido, IV_BYTES, contenido.size - IV_BYTES)
            gson.fromJson(String(json, Charsets.UTF_8), Sesion::class.java)
        }.getOrElse {
            archivo.delete()
            null
        }
    }

    @Synchronized
    fun guardar(sesion: Sesion) {
        val cifrado = Cipher.getInstance(CIFRADO)
        cifrado.init(Cipher.ENCRYPT_MODE, clave())
        val contenido = cifrado.iv + cifrado.doFinal(gson.toJson(sesion).toByteArray(Charsets.UTF_8))
        val salida = archivo.startWrite()
        try {
            salida.write(contenido)
            archivo.finishWrite(salida)
        } catch (error: Exception) {
            archivo.failWrite(salida)
            throw error
        }
        actual.value = sesion
    }

    @Synchronized
    fun cerrar(token: String? = null) {
        if (token != null && actual.value?.token != token) return
        archivo.delete()
        actual.value = null
    }

    private fun clave(): SecretKey {
        val almacén = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (almacén.getKey(ALIAS, null) as? SecretKey)
            ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
                init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build())
                generateKey()
            }
    }

    private companion object {
        const val ALIAS = "macondo.session"
        const val CIFRADO = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
    }
}
