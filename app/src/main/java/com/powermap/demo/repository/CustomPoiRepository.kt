package com.powermap.demo.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.powermap.demo.model.CustomPoi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Local device storage for Custom POIs.
 * Mirrors Flutter's CustomPoiRepository — SharedPreferences backed with GeoJSON import/export.
 */
class CustomPoiRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("powermap_custom_pois_v1", Context.MODE_PRIVATE)

    private val gson = Gson()

    private val _pois = MutableStateFlow<List<CustomPoi>>(emptyList())
    val pois: StateFlow<List<CustomPoi>> = _pois.asStateFlow()

    // ── Initialization ────────────────────────────────────────────

    fun load() {
        val raw = prefs.getString("pois", null) ?: return
        try {
            val arr = JsonParser.parseString(raw).asJsonArray
            _pois.value = arr.mapNotNull { el ->
                try { gson.fromJson(el, CustomPoi::class.java) } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomPoiRepo", "load failed", e)
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────

    fun add(poi: CustomPoi) {
        val list = _pois.value.toMutableList()
        list.removeAll { it.id == poi.id }
        list.add(poi)
        _pois.value = list
        persist()
    }

    fun remove(id: String) {
        _pois.value = _pois.value.filter { it.id != id }
        persist()
    }

    fun contains(id: String): Boolean = _pois.value.any { it.id == id }

    fun findById(id: String): CustomPoi? = _pois.value.firstOrNull { it.id == id }

    /**
     * Finds a saved POI near the given [lat]/[lng] within [toleranceDeg].
     * Used by onMarkerTapped to identify if a tapped marker is a saved POI.
     */
    fun findByCoords(lat: Double, lng: Double, toleranceDeg: Double = 0.00005): CustomPoi? =
        _pois.value.firstOrNull { poi ->
            Math.abs(poi.latitude - lat) < toleranceDeg &&
            Math.abs(poi.longitude - lng) < toleranceDeg
        }

    // ── Export ────────────────────────────────────────────────────

    /** Converts all POIs into a GeoJSON FeatureCollection string. */
    fun exportGeoJson(): String {
        val features = JsonArray()
        _pois.value.forEach { features.add(it.toGeoJsonFeature()) }
        val fc = JsonObject().apply {
            addProperty("type", "FeatureCollection")
            add("features", features)
        }
        return gson.toJson(fc)
    }

    /**
     * Saves GeoJSON export to a file in the app's cache directory.
     * Returns a content URI suitable for sharing via Intent.
     */
    fun saveExportFileUri(): Uri? {
        return try {
            val file = File(context.cacheDir, "powermap_pois_export.geojson")
            file.writeText(exportGeoJson())
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            android.util.Log.e("CustomPoiRepo", "saveExportFileUri failed", e)
            null
        }
    }

    // ── Import ────────────────────────────────────────────────────

    data class ImportResult(val totalFound: Int, val newlyAdded: Int, val updated: Int, val error: Boolean = false)

    /**
     * Parses a GeoJSON string and imports valid Point features into storage.
     * Returns [ImportResult] with counts of found, newly added, and updated POIs.
     */
    fun importGeoJsonString(raw: String): ImportResult {
        return try {
            val json = JsonParser.parseString(raw).asJsonObject
            val features = json.getAsJsonArray("features") ?: return ImportResult(0, 0, 0)
            var totalFound = 0
            var newlyAdded = 0
            var updated = 0
            for (el in features) {
                val feature = el.asJsonObject
                val poi = CustomPoi.fromGeoJsonFeature(feature) ?: continue
                totalFound++
                val list = _pois.value.toMutableList()
                val existingIdx = list.indexOfFirst { it.id == poi.id }
                if (existingIdx >= 0) {
                    list[existingIdx] = poi
                    updated++
                } else {
                    list.add(poi)
                    newlyAdded++
                }
                _pois.value = list
            }
            if (newlyAdded > 0 || updated > 0) persist()
            ImportResult(totalFound, newlyAdded, updated)
        } catch (e: Exception) {
            android.util.Log.e("CustomPoiRepo", "importGeoJsonString failed", e)
            ImportResult(0, 0, 0, error = true)
        }
    }

    /**
     * Reads and imports GeoJSON from a content URI (from file picker).
     * Returns [ImportResult] with counts.
     */
    fun importFromUri(uri: Uri): ImportResult {
        return try {
            val raw = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.readText() ?: return ImportResult(0, 0, 0, error = true)
            importGeoJsonString(raw)
        } catch (e: Exception) {
            android.util.Log.e("CustomPoiRepo", "importFromUri failed", e)
            ImportResult(0, 0, 0, error = true)
        }
    }

    // ── Persistence ───────────────────────────────────────────────

    private fun persist() {
        val arr = JsonArray()
        _pois.value.forEach { arr.add(gson.toJsonTree(it)) }
        prefs.edit().putString("pois", gson.toJson(arr)).apply()
    }
}
