package pt.carlosgoncalves.listarota.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pt.carlosgoncalves.listarota.R
import pt.carlosgoncalves.listarota.databinding.ItemRouteStopBinding
import pt.carlosgoncalves.listarota.model.RouteStop

class StopAdapter(
    private val onToggleCompleted: (RouteStop) -> Unit,
    private val onMoveUp: (Int) -> Unit,
    private val onMoveDown: (Int) -> Unit,
    private val onNavigate: (RouteStop) -> Unit,
    private val onCopyAddress: (RouteStop) -> Unit,
    private val onShareStop: (RouteStop) -> Unit,
    private val onDeleteStop: (RouteStop) -> Unit
) : ListAdapter<RouteStop, StopAdapter.StopViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
        val binding = ItemRouteStopBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StopViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
        holder.bind(getItem(position), position, itemCount)
    }

    inner class StopViewHolder(
        private val binding: ItemRouteStopBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stop: RouteStop, position: Int, totalCount: Int) {
            val context = binding.root.context

            binding.tvOrderNumber.text = context.getString(R.string.stop_order_format, position + 1)

            val hasTitle = stop.title.isNotBlank()
            if (hasTitle) {
                binding.tvTitle.visibility = View.VISIBLE
                binding.tvTitle.text = stop.title
                binding.tvAddress.text = stop.address
            } else {
                binding.tvTitle.visibility = View.GONE
                binding.tvAddress.text = stop.address
            }

            if (stop.note.isNotBlank()) {
                binding.tvNote.visibility = View.VISIBLE
                binding.tvNote.text = stop.note
            } else {
                binding.tvNote.visibility = View.GONE
            }

            // Evitar disparar listener antigo durante recycle
            binding.cbCompleted.setOnCheckedChangeListener(null)
            binding.cbCompleted.isChecked = stop.completed
            binding.cbCompleted.setOnCheckedChangeListener { _, _ ->
                onToggleCompleted(stop)
            }

            // Destaque visual claro quando concluída
            if (stop.completed) {
                binding.cardStop.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.surface_completed)
                )
                binding.cardStop.strokeColor =
                    ContextCompat.getColor(context, R.color.border_completed)
                binding.tvOrderNumber.setBackgroundResource(R.drawable.bg_order_badge_completed)
                binding.tvTitle.paintFlags =
                    binding.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvAddress.paintFlags =
                    binding.tvAddress.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvTitle.alpha = 0.6f
                binding.tvAddress.alpha = 0.65f
                binding.tvNote.alpha = 0.55f
            } else {
                binding.cardStop.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.surface_card)
                )
                binding.cardStop.strokeColor =
                    ContextCompat.getColor(context, R.color.border_default)
                binding.tvOrderNumber.setBackgroundResource(R.drawable.bg_order_badge_active)
                binding.tvTitle.paintFlags =
                    binding.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvAddress.paintFlags =
                    binding.tvAddress.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvTitle.alpha = 1.0f
                binding.tvAddress.alpha = 1.0f
                binding.tvNote.alpha = 1.0f
            }

            binding.btnMoveUp.isEnabled = position > 0
            binding.btnMoveUp.alpha = if (position > 0) 1.0f else 0.35f
            binding.btnMoveUp.setOnClickListener {
                val idx = bindingAdapterPosition
                if (idx != RecyclerView.NO_POSITION) onMoveUp(idx)
            }

            binding.btnMoveDown.isEnabled = position < totalCount - 1
            binding.btnMoveDown.alpha = if (position < totalCount - 1) 1.0f else 0.35f
            binding.btnMoveDown.setOnClickListener {
                val idx = bindingAdapterPosition
                if (idx != RecyclerView.NO_POSITION) onMoveDown(idx)
            }

            binding.btnDelete.setOnClickListener { onDeleteStop(stop) }
            binding.btnNavigate.setOnClickListener { onNavigate(stop) }
            binding.btnCopy.setOnClickListener { onCopyAddress(stop) }
            binding.btnShare.setOnClickListener { onShareStop(stop) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<RouteStop>() {
        override fun areItemsTheSame(oldItem: RouteStop, newItem: RouteStop): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RouteStop, newItem: RouteStop): Boolean {
            return oldItem == newItem
        }
    }
}
