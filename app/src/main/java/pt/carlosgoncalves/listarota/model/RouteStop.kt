package pt.carlosgoncalves.listarota.model

import java.util.UUID

data class RouteStop(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val address: String,
    val note: String = "",
    val completed: Boolean = false
)
