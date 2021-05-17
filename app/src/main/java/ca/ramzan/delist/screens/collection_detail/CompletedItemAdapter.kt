package ca.ramzan.delist.screens.collection_detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.ramzan.delist.R
import ca.ramzan.delist.databinding.ListItemCollectionDetailBinding
import ca.ramzan.delist.databinding.ListItemCompletedHeaderBinding
import ca.ramzan.delist.databinding.ListItemCompletedTaskBinding
import ca.ramzan.delist.room.CompletedTaskDisplay

private const val ITEM_VIEW_TYPE_DETAIL = 0
private const val ITEM_VIEW_TYPE_HEADER = 1
private const val ITEM_VIEW_TYPE_COMPLETED_TASK = 2


class CollectionDetailAdapter(private val onClickListener: OnClickListener) :
    ListAdapter<CollectionDetailListItem, RecyclerView.ViewHolder>(
        CollectionDetailListItemDiffCallback()
    ) {

    interface OnClickListener {
        fun onCreateTask(tasks: String)
        fun onCompleteTask()
    }

    class ListItemCompletedTaskViewHolder(private val binding: ListItemCompletedTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CollectionDetailListItem.CompletedTask) {
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

    class ListItemCompletedHeaderViewHolder(binding: ListItemCompletedHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): ListItemCompletedHeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemCompletedHeaderBinding.inflate(layoutInflater, parent, false)
                return ListItemCompletedHeaderViewHolder(binding)
            }
        }
    }

    class ListItemCollectionDetailViewHolder(private val binding: ListItemCollectionDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(currentTask: String?, onClickListener: OnClickListener) {
            if (currentTask != null) {
                binding.currentTask.text = currentTask
                binding.completeTaskButton.isEnabled = true
            } else {
                binding.currentTask.text =
                    binding.root.context.getString(R.string.empty_collection_message)
                binding.completeTaskButton.isEnabled = false
            }
            binding.completeTaskButton.setOnClickListener {
                it.isEnabled = false
                onClickListener.onCompleteTask()
            }
            binding.createTaskButton.setOnClickListener {
//                binding.newTaskInput.text = null
//                onClickListener.onCreateTask(binding.newTaskInput.text.toString())
            }
        }

        companion object {
            fun from(parent: ViewGroup): ListItemCollectionDetailViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemCollectionDetailBinding.inflate(layoutInflater, parent, false)
                return ListItemCollectionDetailViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ListItemCompletedTaskViewHolder -> holder.bind(
                item as CollectionDetailListItem.CompletedTask
            )
            is ListItemCompletedHeaderViewHolder -> {
                /* no-op */
            }
            is ListItemCollectionDetailViewHolder -> {
                holder.bind(
                    (item as CollectionDetailListItem.DetailItem).currentTask,
                    onClickListener
                )
            }
            else -> throw Exception("Illegal holder type: $holder")

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_DETAIL -> ListItemCollectionDetailViewHolder.from(parent)
            ITEM_VIEW_TYPE_HEADER -> ListItemCompletedHeaderViewHolder.from(parent)
            ITEM_VIEW_TYPE_COMPLETED_TASK -> ListItemCompletedTaskViewHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is CollectionDetailListItem.CompletedTask -> ITEM_VIEW_TYPE_COMPLETED_TASK
            is CollectionDetailListItem.DetailItem -> ITEM_VIEW_TYPE_DETAIL
            CollectionDetailListItem.CompletedHeader -> ITEM_VIEW_TYPE_HEADER
        }
    }


    fun submitNoCompleted(taskString: String?) {
        submitList(listOf(CollectionDetailListItem.DetailItem(taskString)))
    }

    fun submitWithCompleted(
        taskString: String?,
        completedTasks: List<CompletedTaskDisplay>
    ) {
        submitList(
            listOf(
                CollectionDetailListItem.DetailItem(taskString),
                CollectionDetailListItem.CompletedHeader
            ) + completedTasks.map { CollectionDetailListItem.CompletedTask(it) }
        )
    }
}

class CollectionDetailListItemDiffCallback : DiffUtil.ItemCallback<CollectionDetailListItem>() {
    override fun areItemsTheSame(
        oldItem: CollectionDetailListItem,
        newItem: CollectionDetailListItem
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: CollectionDetailListItem,
        newItem: CollectionDetailListItem
    ): Boolean {
        return when (oldItem) {
            is CollectionDetailListItem.CompletedTask -> oldItem.content == (newItem as CollectionDetailListItem.CompletedTask).content
            is CollectionDetailListItem.DetailItem -> oldItem.currentTask == (newItem as CollectionDetailListItem.DetailItem).currentTask
            is CollectionDetailListItem.CompletedHeader -> true
        }
    }
}

sealed class CollectionDetailListItem {
    abstract val id: Long

    class DetailItem(val currentTask: String?) : CollectionDetailListItem() {
        override val id = -1L
    }

    object CompletedHeader : CollectionDetailListItem() {
        override val id = -2L
    }

    class CompletedTask(data: CompletedTaskDisplay) : CollectionDetailListItem() {
        override val id = data.id
        val content = data.content
    }
}