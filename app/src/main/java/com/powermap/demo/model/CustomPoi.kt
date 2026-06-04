package com.powermap.demo.model

import com.google.gson.JsonObject
import com.powermap.sdk.LatLng
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data model for a user-saved custom POI.
 * Mirrors Flutter's CustomPoi exactly for GeoJSON cross-compatibility.
 */
data class CustomPoi(
    val id: String,
    val name: String,
    val note: String? = null,
    val latitude: Double,
    val longitude: Double,
    val color: String = "#FF5722",
    val icon: String? = null,
    val createdAt: String, // ISO-8601
    val address: String? = null,
    val tambon: String? = null,
    val amphoe: String? = null,
    val province: String? = null
) {
    val latLng: LatLng get() = LatLng(latitude, longitude)

    val displayAddress: String
        get() {
            val parts = listOf(tambon, amphoe, province).filter { !it.isNullOrEmpty() }
            return if (parts.isNotEmpty()) parts.joinToString(", ")
            else address ?: ""
        }

    // ── GeoJSON ─────────────────────────────────────────────────

    fun toGeoJsonFeature(): JsonObject {
        return JsonObject().apply {
            addProperty("type", "Feature")
            add("geometry", JsonObject().apply {
                addProperty("type", "Point")
                add("coordinates", com.google.gson.JsonArray().apply {
                    add(longitude); add(latitude)
                })
            })
            add("properties", JsonObject().apply {
                addProperty("id", id)
                addProperty("name", name)
                addProperty("color", color)
                addProperty("createdAt", createdAt)
                // Only write optional fields when non-null to avoid JsonNull
                note?.let { addProperty("note", it) }
                icon?.let { addProperty("icon", it) }
                address?.let { addProperty("address", it) }
                tambon?.let { addProperty("tambon", it) }
                amphoe?.let { addProperty("amphoe", it) }
                province?.let { addProperty("province", it) }
            })
        }
    }

    companion object {
        /** Safely read a String property from a JsonObject, returning null for missing or JsonNull values. */
        private fun JsonObject.optString(key: String): String? {
            val el = get(key) ?: return null
            return if (el.isJsonNull) null else el.asString
        }

        fun fromGeoJsonFeature(feature: JsonObject): CustomPoi? {
            return try {
                val geom = feature.getAsJsonObject("geometry")
                if (geom?.getAsJsonPrimitive("type")?.asString != "Point") return null
                val coords = geom.getAsJsonArray("coordinates")
                val lon = coords[0].asDouble
                val lat = coords[1].asDouble
                val props = feature.getAsJsonObject("properties") ?: JsonObject()
                CustomPoi(
                    id = props.optString("id") ?: System.currentTimeMillis().toString(),
                    name = props.optString("name") ?: "Imported POI",
                    note = props.optString("note"),
                    latitude = lat,
                    longitude = lon,
                    color = props.optString("color") ?: "#FF5722",
                    icon = props.optString("icon"),
                    createdAt = props.optString("createdAt") ?: "",
                    address = props.optString("address"),
                    tambon = props.optString("tambon"),
                    amphoe = props.optString("amphoe"),
                    province = props.optString("province")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

object PoiIconMapper {
    fun getIcon(name: String?): ImageVector {
        return when (name) {
            "home_rounded" -> Icons.Rounded.Home
            "work_rounded" -> Icons.Rounded.Build
            "restaurant_rounded" -> Icons.Rounded.ShoppingCart
            "shopping_cart_rounded" -> Icons.Rounded.ShoppingCart
            "favorite_rounded" -> Icons.Rounded.Favorite
            "star_rounded" -> Icons.Rounded.Star
            "location_on_rounded" -> Icons.Rounded.LocationOn
            else -> Icons.Rounded.LocationOn
        }
    }
}
