package com.diligenciard.app.data

import android.content.Context
import com.diligenciard.app.data.model.CategoryDef
import com.diligenciard.app.data.model.ServiceDef
import com.diligenciard.app.data.model.ServicesCatalogFile
import kotlinx.serialization.json.Json

/**
 * Catálogo local de diligencias dominicanas (spec §9 paso 1).
 * Interpreta la intención del usuario: diferencia diligencia ("renovar licencia")
 * de tipo de comercio ("farmacia") o nombre concreto.
 */
class ServicesCatalog private constructor(private val file: ServicesCatalogFile) {

    val services: List<ServiceDef> get() = file.services
    val categories: Map<String, CategoryDef> get() = file.categories

    fun serviceById(id: String): ServiceDef? = file.services.find { it.id == id }

    /** Devuelve el servicio cuyo keyword mejor coincide con la consulta, o null si es búsqueda libre. */
    fun matchIntent(query: String): ServiceDef? {
        val q = normalize(query)
        if (q.isBlank()) return null
        return file.services
            .flatMap { svc -> svc.keywords.map { kw -> svc to normalize(kw) } }
            .filter { (_, kw) -> q.contains(kw) || kw.contains(q) }
            .maxByOrNull { (_, kw) -> kw.length }
            ?.first
    }

    private fun normalize(s: String): String =
        s.lowercase()
            .replace('á', 'a').replace('é', 'e').replace('í', 'i')
            .replace('ó', 'o').replace('ú', 'u').replace('ñ', 'n')
            .trim()

    companion object {
        @Volatile
        private var instance: ServicesCatalog? = null

        fun get(context: Context): ServicesCatalog =
            instance ?: synchronized(this) {
                instance ?: load(context).also { instance = it }
            }

        private fun load(context: Context): ServicesCatalog {
            val json = context.assets.open("services_rd.json").bufferedReader().use { it.readText() }
            val parser = Json { ignoreUnknownKeys = true }
            return ServicesCatalog(parser.decodeFromString<ServicesCatalogFile>(json))
        }
    }
}
