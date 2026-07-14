package com.diligenciard.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * SHA-1 del certificado de firma, en hex mayúsculas sin dos puntos.
 * Requerido en el encabezado X-Android-Cert para que Google valide
 * la clave API restringida a esta app.
 */
object AppSignature {

    @Volatile
    private var cached: String? = null

    fun sha1(context: Context): String =
        cached ?: synchronized(this) {
            cached ?: compute(context).also { cached = it }
        }

    private fun compute(context: Context): String {
        val pm = context.packageManager
        val packageName = context.packageName
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            info.signingInfo?.apkContentsSigners ?: emptyArray()
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures ?: emptyArray()
        }
        val cert = signatures.firstOrNull() ?: return ""
        val digest = MessageDigest.getInstance("SHA-1").digest(cert.toByteArray())
        return digest.joinToString("") { "%02X".format(it) }
    }
}
