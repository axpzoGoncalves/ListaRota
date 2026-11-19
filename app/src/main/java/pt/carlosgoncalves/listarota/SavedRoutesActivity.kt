package pt.carlosgoncalves.listarota

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import pt.carlosgoncalves.listarota.data.RouteStorage
import pt.carlosgoncalves.listarota.databinding.ActivitySavedRoutesBinding
import pt.carlosgoncalves.listarota.model.SavedRoute
import pt.carlosgoncalves.listarota.ui.SavedRouteAdapter

class SavedRoutesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedRoutesBinding
    private lateinit var storage: RouteStorage
    private lateinit var adapter: SavedRouteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedRoutesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storage = RouteStorage(this)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        adapter = SavedRouteAdapter(
            onLoadRoute = { route -> confirmLoadRoute(route) },
            onDeleteRoute = { route -> confirmDeleteRoute(route) }
        )

        binding.recyclerSavedRoutes.layoutManager = LinearLayoutManager(this)
        binding.recyclerSavedRoutes.adapter = adapter

        refreshRoutes()
    }

    private fun refreshRoutes() {
        val routes = storage.loadSavedRoutes()
        adapter.submitList(routes)

        if (routes.isEmpty()) {
            binding.recyclerSavedRoutes.visibility = View.GONE
            binding.layoutEmptySaved.visibility = View.VISIBLE
        } else {
            binding.recyclerSavedRoutes.visibility = View.VISIBLE
            binding.layoutEmptySaved.visibility = View.GONE
        }
    }

    private fun confirmLoadRoute(route: SavedRoute) {
        val currentStops = storage.loadCurrentStops()
        if (currentStops.isEmpty()) {
            applyLoadedRoute(route)
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_load_route_title)
            .setMessage(getString(R.string.dialog_load_route_message, route.name))
            .setPositiveButton(R.string.btn_load) { _, _ ->
                applyLoadedRoute(route)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun applyLoadedRoute(route: SavedRoute) {
        storage.saveCurrentStops(route.stops)
        val resultIntent = Intent().apply {
            putExtra(EXTRA_LOADED_ROUTE_NAME, route.name)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun confirmDeleteRoute(route: SavedRoute) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_route_title)
            .setMessage(getString(R.string.dialog_delete_route_message, route.name))
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                storage.deleteSavedRoute(route.id)
                refreshRoutes()
                Snackbar.make(binding.root, R.string.msg_saved_route_deleted, Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.footerInclude.root)
                    .show()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    companion object {
        const val EXTRA_LOADED_ROUTE_NAME = "extra_loaded_route_name"
    }
}
