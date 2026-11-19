package pt.carlosgoncalves.listarota.model

import java.util.UUID

data class SavedRoute(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAtFormatted: String,
    val stops: List<RouteStop>
)
