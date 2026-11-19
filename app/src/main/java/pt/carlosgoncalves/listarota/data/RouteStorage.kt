package pt.carlosgoncalves.listarota.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import pt.carlosgoncalves.listarota.model.RouteStop
import pt.carlosgoncalves.listarota.model.SavedRoute
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class RouteStorage(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadCurrentStops(): List<RouteStop> {
        val raw = prefs.getString(KEY_CURRENT_STOPS, null) ?: return emptyList()
        return parseStopsArray(raw)
    }

    fun saveCurrentStops(stops: List<RouteStop>) {
        val jsonArray = stopsToJsonArray(stops)
        prefs.edit().putString(KEY_CURRENT_STOPS, jsonArray.toString()).apply()
    }

    fun loadSavedRoutes(): List<SavedRoute> {
        val raw = prefs.getString(KEY_SAVED_ROUTES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id", UUID.randomUUID().toString())
                    val name = obj.optString("name", "").trim()
                    val createdAt = obj.optString("createdAtFormatted", "")
                    val stopsArr = obj.optJSONArray("stops") ?: JSONArray()
                    val stops = parseStopsJsonArray(stopsArr)
                    if (name.isNotEmpty()) {
                        add(
                            SavedRoute(
                                id = id,
                                name = name,
                                createdAtFormatted = createdAt,
                                stops = stops
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveNamedRoute(name: String, stops: List<RouteStop>): SavedRoute {
        val cleanName = name.trim()
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val timestamp = formatter.format(Date())

        val clonedStops = stops.map {
            RouteStop(
                id = UUID.randomUUID().toString(),
                title = it.title,
                address = it.address,
                note = it.note,
                completed = false
            )
        }

        val newRoute = SavedRoute(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            createdAtFormatted = timestamp,
            stops = clonedStops
        )

        val updated = listOf(newRoute) + loadSavedRoutes()
        persistSavedRoutes(updated)
        return newRoute
    }

    fun deleteSavedRoute(routeId: String) {
        val updated = loadSavedRoutes().filterNot { it.id == routeId }
        persistSavedRoutes(updated)
    }

    private fun persistSavedRoutes(routes: List<SavedRoute>) {
        val array = JSONArray()
        routes.forEach { route ->
            val obj = JSONObject().apply {
                put("id", route.id)
                put("name", route.name)
                put("createdAtFormatted", route.createdAtFormatted)
                put("stops", stopsToJsonArray(route.stops))
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_ROUTES, array.toString()).apply()
    }

    private fun stopsToJsonArray(stops: List<RouteStop>): JSONArray {
        val array = JSONArray()
        stops.forEach { stop ->
            val obj = JSONObject().apply {
                put("id", stop.id)
                put("title", stop.title)
                put("address", stop.address)
                put("note", stop.note)
                put("completed", stop.completed)
            }
            array.put(obj)
        }
        return array
    }

    private fun parseStopsArray(raw: String): List<RouteStop> {
        return try {
            parseStopsJsonArray(JSONArray(raw))
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseStopsJsonArray(array: JSONArray): List<RouteStop> {
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val address = obj.optString("address", "").trim()
                if (address.isNotEmpty()) {
                    add(
                        RouteStop(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", ""),
                            address = address,
                            note = obj.optString("note", ""),
                            completed = obj.optBoolean("completed", false)
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "listarota_local_storage"
        private const val KEY_CURRENT_STOPS = "current_stops_json"
        private const val KEY_SAVED_ROUTES = "saved_routes_json"

        fun parseBatchAddresses(rawText: String): List<RouteStop> {
            return rawText
                .lineSequence()
                .map { line -> line.trim().replace(Regex("\\s+"), " ") }
                .filter { line -> line.isNotEmpty() }
                .map { cleanAddress ->
                    RouteStop(
                        id = UUID.randomUUID().toString(),
                        address = cleanAddress
                    )
                }
                .toList()
        }
    }
}
