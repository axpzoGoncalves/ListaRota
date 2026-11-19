package pt.carlosgoncalves.listarota

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import pt.carlosgoncalves.listarota.data.RouteStorage
import pt.carlosgoncalves.listarota.databinding.ActivityMainBinding
import pt.carlosgoncalves.listarota.databinding.DialogAddStopBinding
import pt.carlosgoncalves.listarota.databinding.DialogBatchImportBinding
import pt.carlosgoncalves.listarota.databinding.DialogSaveRouteBinding
import pt.carlosgoncalves.listarota.model.RouteStop
import pt.carlosgoncalves.listarota.ui.StopAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var storage: RouteStorage
    private lateinit var adapter: StopAdapter

    private var stops: MutableList<RouteStop> = mutableListOf()

    private val savedRoutesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            reloadStopsFromStorage()
            val loadedName = result.data?.getStringExtra(SavedRoutesActivity.EXTRA_LOADED_ROUTE_NAME)
            if (!loadedName.isNullOrBlank()) {
                showSnackbar(getString(R.string.route_loaded_feedback, loadedName, stops.size))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storage = RouteStorage(this)

        setupToolbar()
        setupRecyclerView()
        setupButtons()
        reloadStopsFromStorage()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_save_route -> {
                    showSaveRouteDialog()
                    true
                }
                R.id.action_saved_routes -> {
                    val intent = Intent(this, SavedRoutesActivity::class.java)
                    savedRoutesLauncher.launch(intent)
                    true
                }
                R.id.action_clear_completed -> {
                    clearCompletedStops()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = StopAdapter(
            onToggleCompleted = { stop -> toggleStopCompleted(stop) },
            onMoveUp = { index -> moveStop(index, index - 1) },
            onMoveDown = { index -> moveStop(index, index + 1) },
            onNavigate = { stop -> openInMaps(stop) },
            onCopyAddress = { stop -> copyAddress(stop) },
            onShareStop = { stop -> shareStop(stop) },
            onDeleteStop = { stop -> deleteStop(stop) }
        )
        binding.recyclerStops.layoutManager = LinearLayoutManager(this)
        binding.recyclerStops.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnBatchImport.setOnClickListener { showBatchImportDialog() }
        binding.btnAddSingle.setOnClickListener { showAddStopDialog() }
        binding.btnEmptyBatchImport.setOnClickListener { showBatchImportDialog() }
        binding.btnClearCompletedInline.setOnClickListener { clearCompletedStops() }
    }

    private fun reloadStopsFromStorage() {
        stops = storage.loadCurrentStops().toMutableList()
        renderStops()
    }

    private fun persistAndRender() {
        storage.saveCurrentStops(stops)
        renderStops()
    }

    private fun renderStops() {
        adapter.submitList(stops.toList())

        val total = stops.size
        val completed = stops.count { it.completed }

        binding.tvProgressSummary.text = getString(R.string.stops_progress, completed, total)
        binding.btnClearCompletedInline.visibility = if (completed > 0) View.VISIBLE else View.GONE

        if (stops.isEmpty()) {
            binding.recyclerStops.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.recyclerStops.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun showBatchImportDialog() {
        val dialogBinding = DialogBatchImportBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_batch_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_add_stops) { _, _ ->
                val raw = dialogBinding.etBatchAddresses.text?.toString().orEmpty()
                val newStops = RouteStorage.parseBatchAddresses(raw)
                if (newStops.isEmpty()) {
                    showSnackbar(getString(R.string.msg_no_valid_addresses))
                } else {
                    stops.addAll(newStops)
                    persistAndRender()
                    showSnackbar(getString(R.string.added_stops_count, newStops.size))
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showAddStopDialog() {
        val dialogBinding = DialogAddStopBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_add_stop_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val title = dialogBinding.etStopTitle.text?.toString()?.trim().orEmpty()
                val address = dialogBinding.etStopAddress.text?.toString()?.trim().orEmpty()
                val note = dialogBinding.etStopNote.text?.toString()?.trim().orEmpty()

                if (address.isEmpty()) {
                    dialogBinding.tilStopAddress.error = getString(R.string.error_address_required)
                    return@setOnClickListener
                }

                dialogBinding.tilStopAddress.error = null
                stops.add(
                    RouteStop(
                        title = title,
                        address = address,
                        note = note
                    )
                )
                persistAndRender()
                dialog.dismiss()
                showSnackbar(getString(R.string.msg_stop_added))
            }
        }

        dialog.show()
    }

    private fun showSaveRouteDialog() {
        if (stops.isEmpty()) {
            showSnackbar(getString(R.string.msg_empty_route_cannot_save))
            return
        }

        val dialogBinding = DialogSaveRouteBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_save_route_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = dialogBinding.etRouteName.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    dialogBinding.tilRouteName.error = getString(R.string.error_route_name_required)
                    return@setOnClickListener
                }
                dialogBinding.tilRouteName.error = null
                val saved = storage.saveNamedRoute(name, stops)
                dialog.dismiss()
                showSnackbar(getString(R.string.msg_route_saved, saved.name))
            }
        }

        dialog.show()
    }

    private fun toggleStopCompleted(stop: RouteStop) {
        val idx = stops.indexOfFirst { it.id == stop.id }
        if (idx == -1) return
        stops[idx] = stops[idx].copy(completed = !stops[idx].completed)
        persistAndRender()
    }

    private fun moveStop(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in stops.indices || toIndex !in stops.indices) return
        val item = stops.removeAt(fromIndex)
        stops.add(toIndex, item)
        persistAndRender()
    }

    private fun deleteStop(stop: RouteStop) {
        val idx = stops.indexOfFirst { it.id == stop.id }
        if (idx == -1) return
        val removed = stops.removeAt(idx)
        persistAndRender()
        Snackbar.make(binding.root, R.string.msg_stop_deleted, Snackbar.LENGTH_LONG)
            .setAnchorView(binding.footerInclude.root)
            .setAction(R.string.btn_undo) {
                val restoreIndex = idx.coerceAtMost(stops.size)
                stops.add(restoreIndex, removed)
                persistAndRender()
            }
            .show()
    }

    private fun clearCompletedStops() {
        val completedCount = stops.count { it.completed }
        if (completedCount == 0) {
            showSnackbar(getString(R.string.msg_no_completed_to_clear))
            return
        }
        stops.removeAll { it.completed }
        persistAndRender()
        showSnackbar(getString(R.string.cleared_completed_count, completedCount))
    }

    private fun openInMaps(stop: RouteStop) {
        val encoded = Uri.encode(stop.address)
        val googleMapsIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:0,0?q=$encoded")
        ).apply {
            setPackage("com.google.android.apps.maps")
        }

        try {
            startActivity(googleMapsIntent)
        } catch (_: ActivityNotFoundException) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded"))
            try {
                startActivity(fallbackIntent)
            } catch (_: ActivityNotFoundException) {
                showSnackbar(getString(R.string.msg_no_maps_app))
            }
        }
    }

    private fun copyAddress(stop: RouteStop) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Morada", stop.address))
        showSnackbar(getString(R.string.msg_address_copied))
    }

    private fun shareStop(stop: RouteStop) {
        val header = if (stop.title.isNotBlank()) stop.title else getString(R.string.share_default_header)
        val message = if (stop.note.isNotBlank()) {
            getString(R.string.share_stop_with_note_format, header, stop.address, stop.note)
        } else {
            getString(R.string.share_stop_format, header, stop.address)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_chooser_title)))
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.footerInclude.root)
            .show()
    }
}
