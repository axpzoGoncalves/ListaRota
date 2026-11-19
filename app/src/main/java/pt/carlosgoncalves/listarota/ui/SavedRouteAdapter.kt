package pt.carlosgoncalves.listarota.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pt.carlosgoncalves.listarota.R
import pt.carlosgoncalves.listarota.databinding.ItemSavedRouteBinding
import pt.carlosgoncalves.listarota.model.SavedRoute

class SavedRouteAdapter(
    private val onLoadRoute: (SavedRoute) -> Unit,
    private val onDeleteRoute: (SavedRoute) -> Unit
) : ListAdapter<SavedRoute, SavedRouteAdapter.SavedRouteViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedRouteViewHolder {
        val binding = ItemSavedRouteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SavedRouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavedRouteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SavedRouteViewHolder(
        private val binding: ItemSavedRouteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(route: SavedRoute) {
            val context = binding.root.context
            binding.tvRouteName.text = route.name
            binding.tvRouteMeta.text = context.getString(
                R.string.saved_route_meta,
                route.stops.size,
                route.createdAtFormatted
            )

            binding.btnLoadRoute.setOnClickListener { onLoadRoute(route) }
            binding.btnDeleteRoute.setOnClickListener { onDeleteRoute(route) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SavedRoute>() {
        override fun areItemsTheSame(oldItem: SavedRoute, newItem: SavedRoute): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SavedRoute, newItem: SavedRoute): Boolean {
            return oldItem == newItem
        }
    }
}
