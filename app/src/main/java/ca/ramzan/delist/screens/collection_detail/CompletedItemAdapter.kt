package ca.ramzan.delist.screens.collection_detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.ramzan.delist.databinding.ListItemCompletedItemBinding
import ca.ramzan.delist.room.CompletedItemDisplay

class CompletedItemAdapter :
    ListAdapter<CompletedItemDisplay, CompletedItemAdapter.ListItemCompletedItemViewHolder>(
        CompletedItemDiffCallback()
    ) {

    class ListItemCompletedItemViewHolder(private val binding: ListItemCompletedItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CompletedItemDisplay) {
            binding.completedItemText.text = item.content
        }

        companion object {
            fun from(parent: ViewGroup): ListItemCompletedItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemCompletedItemBinding.inflate(layoutInflater, parent, false)
                return ListItemCompletedItemViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: ListItemCompletedItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListItemCompletedItemViewHolder {
        return ListItemCompletedItemViewHolder.from(parent)
    }
}

class CompletedItemDiffCallback : DiffUtil.ItemCallback<CompletedItemDisplay>() {
    override fun areItemsTheSame(
        oldItem: CompletedItemDisplay,
        newItem: CompletedItemDisplay
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: CompletedItemDisplay,
        newItem: CompletedItemDisplay
    ): Boolean {
        return oldItem.content == newItem.content
    }
}