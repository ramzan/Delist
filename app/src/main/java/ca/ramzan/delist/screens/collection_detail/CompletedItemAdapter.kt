package ca.ramzan.delist.screens.collection_detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.ramzan.delist.R
import ca.ramzan.delist.databinding.ListItemCompletedTaskBinding
import ca.ramzan.delist.room.CompletedTaskDisplay

class CompletedTaskAdapter :
    ListAdapter<CompletedTaskDisplay, CompletedTaskAdapter.ListItemCompletedTaskViewHolder>(
        CompletedTaskDiffCallback()
    ) {

    class ListItemCompletedTaskViewHolder(private val binding: ListItemCompletedTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CompletedTaskDisplay) {
            binding.completedTaskText.text =
                binding.root.context.getString(R.string.bulleted_list_item, item.content)
        }

        companion object {
            fun from(parent: ViewGroup): ListItemCompletedTaskViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemCompletedTaskBinding.inflate(layoutInflater, parent, false)
                return ListItemCompletedTaskViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: ListItemCompletedTaskViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListItemCompletedTaskViewHolder {
        return ListItemCompletedTaskViewHolder.from(parent)
    }
}

class CompletedTaskDiffCallback : DiffUtil.ItemCallback<CompletedTaskDisplay>() {
    override fun areItemsTheSame(
        oldItem: CompletedTaskDisplay,
        newItem: CompletedTaskDisplay
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: CompletedTaskDisplay,
        newItem: CompletedTaskDisplay
    ): Boolean {
        return oldItem.content == newItem.content
    }
}