package com.powermap.demo.measure

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.powermap.sdk.LatLng
import com.powermap.sdk.PowerMapController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * Manages Measure Distance state and map sync for Android.
 *
 * Features:
 * - Auto-zoom (zoom 17) when entering measure mode
 * - Rubber-band preview line: last confirmed point → current camera center
 * - Live distance = confirmed distance + preview segment length
 *
 * Uses verified PowerMap SDK APIs:
 *   - ctrl.markers.addMarker / clearMarkers
 *   - ctrl.geometry.importGeoJson / removeGeometry
 *   - ctrl.moveCamera / getCenter
 */
class MeasureDistanceManager(
    private val controller: PowerMapController,
    private val scope: CoroutineScope
) {

    companion object {
        private const val LINE_COLOR    = "#FF5722"   // AppTheme.primary
        private const val LINE_SRC_ID   = "measure-line-src"
        private const val PREV_SRC_ID   = "measure-preview-src"
    }

    // ── Compose-observable state ──────────────────────────────────────────────
    var isMeasuring by mutableStateOf(false)
        private set

    val points = mutableStateListOf<LatLng>()

    /** Camera center updates in real-time; drives live distance display */
    var cameraCenter by mutableStateOf<LatLng?>(null)
        private set

    // ── Internal tracking ─────────────────────────────────────────────────────
    private var lineExists    = false
    private var previewIndex  = 0
    private var previewExists = false
    private var previewJob: Job? = null

    // ─── Public API ──────────────────────────────────────────────────────────

    fun startMeasureMode(seedPoint: LatLng? = null) {
        isMeasuring = true
        points.clear()
        if (seedPoint != null) points.add(seedPoint)
        // Auto-zoom to seed point at zoom 17
        scope.launch {
            try {
                if (seedPoint != null) {
                    controller.moveCamera(seedPoint, zoom = 17.0)
                }
            } catch (_: Exception) {}
            syncToMap()
            startPreviewLoop()
        }
    }

    fun addPoint(latLng: LatLng) {
        if (!isMeasuring) return
        points.add(latLng)
        scope.launch { syncToMap() }
    }

    fun undoLast() {
        if (points.size > 1) {
            points.removeLast()
            scope.launch { syncToMap() }
        }
    }

    fun clearPoints() {
        points.clear()
        scope.launch { syncToMap() }
    }

    fun stopMeasureMode() {
        isMeasuring = false
        points.clear()
        cameraCenter = null
        previewJob?.cancel()
        previewJob = null
        scope.launch { syncToMap() }
    }

    // ─── Distance ─────────────────────────────────────────────────────────────

    fun confirmedDistanceKm(): Double {
        if (points.size < 2) return 0.0
        return (1 until points.size).sumOf { haversine(points[it - 1], points[it]) }
    }

    /** Live distance = confirmed + rubber-band segment to camera center */
    fun liveDistanceKm(): Double {
        val base = confirmedDistanceKm()
        val cam  = cameraCenter ?: return base
        if (points.isEmpty()) return base
        return base + haversine(points.last(), cam)
    }

    fun formatLiveDistance(): String {
        val km = liveDistanceKm()
        return if (km < 1.0) "${(km * 1000).roundToInt()} ม."
        else "${"%.1f".format(km)} กม."
    }

    /** Preview segment distance string e.g. "+ 120 ม." */
    fun formatPreviewSegment(): String {
        val cam = cameraCenter ?: return ""
        if (points.isEmpty()) return ""
        val d = haversine(points.last(), cam)
        return if (d < 1.0) "+ ${(d * 1000).roundToInt()} ม."
        else "+ ${"%.1f".format(d)} กม."
    }

    private fun haversine(a: LatLng, b: LatLng): Double {
        val r = 6371.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val sinDLat = sin(dLat / 2)
        val sinDLon = sin(dLon / 2)
        val x = sinDLat * sinDLat +
                cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sinDLon * sinDLon
        return r * 2 * atan2(sqrt(x), sqrt(1 - x))
    }

    // ─── Preview Loop (polls camera center at ~10fps) ─────────────────────────

    private fun startPreviewLoop() {
        previewJob?.cancel()
        previewJob = scope.launch {
            while (isActive && isMeasuring) {
                try {
                    val center = controller.getCenter()
                    if (center != null && center != cameraCenter) {
                        cameraCenter = center
                        syncPreviewLine()
                    }
                } catch (_: Exception) {}
                delay(32L) // ~30fps
            }
        }
    }

    // ─── Map Sync: confirmed polyline + point markers ─────────────────────────

    private suspend fun syncToMap() {
        // Remove confirmed polyline
        if (lineExists) {
            try { controller.geometry.removeGeometry(LINE_SRC_ID) } catch (_: Exception) {}
            lineExists = false
        }
        // Remove preview line
        if (previewExists) {
            try { controller.geometry.removeGeometry("${PREV_SRC_ID}_0") } catch (_: Exception) {}
            try { controller.geometry.removeGeometry("${PREV_SRC_ID}_1") } catch (_: Exception) {}
            previewExists = false
        }

        // NOTE: Markers (seed = orange circle, confirmed = white circle) are managed
        // by MainMapScreen's LaunchedEffect via addViewMarker for proper visibility.

        if (points.isEmpty()) return

        // Draw confirmed polyline (≥ 2 points)
        if (points.size >= 2) {
            val coords = points.map { listOf(it.longitude, it.latitude) }
            try {
                controller.geometry.importGeoJson(
                    sourceId   = LINE_SRC_ID,
                    geoJson    = buildLineGeoJson(coords, lineWidth = 3.5),
                    autoRender = true,
                    options    = mapOf("format" to "standard")
                )
                lineExists = true
            } catch (_: Exception) {}
        }
    }

    // ─── Map Sync: rubber-band preview line ───────────────────────────────────

    private suspend fun syncPreviewLine() {
        val cam = cameraCenter ?: return
        if (points.isEmpty()) return
        val last = points.last()
        // Draw even for very small distances (threshold removed so it shows immediately)
        if (haversine(last, cam) < 0.00001) return

        val nextIndex = (previewIndex + 1) % 2
        val newSrc = "${PREV_SRC_ID}_$nextIndex"
        val oldSrc = "${PREV_SRC_ID}_$previewIndex"

        val coords = listOf(
            listOf(last.longitude, last.latitude),
            listOf(cam.longitude, cam.latitude)
        )
        try {
            controller.geometry.importGeoJson(
                sourceId   = newSrc,
                geoJson    = buildLineGeoJson(coords, lineWidth = 2.0),
                autoRender = true,
                options    = mapOf("format" to "standard")
            )
            // Remove the old frame ONLY AFTER the new one is drawn
            if (previewExists) {
                try { controller.geometry.removeGeometry(oldSrc) } catch (_: Exception) {}
            }
            previewIndex = nextIndex
            previewExists = true
        } catch (_: Exception) {}
    }

    private fun buildLineGeoJson(
        coords: List<List<Double>>,
        lineWidth: Double = 3.5
    ): Map<String, Any> = mapOf(
        "type" to "FeatureCollection",
        "features" to listOf(
            mapOf(
                "type" to "Feature",
                "geometry" to mapOf(
                    "type" to "LineString",
                    "coordinates" to coords
                ),
                "properties" to mapOf(
                    "_internal" to mapOf(
                        "lineColor" to LINE_COLOR,
                        "lineWidth" to lineWidth,
                        "isHidden"  to false
                    )
                )
            )
        )
    )
}
