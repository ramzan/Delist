package ca.ramzan.delist.screens.collection_detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.ramzan.delist.R
import ca.ramzan.delist.databinding.ListItemCollectionDetailBinding
import ca.ramzan.delist.databinding.ListItemCompletedHeaderBinding
import ca.ramzan.delist.databinding.ListItemCompletedTaskBinding
import ca.ramzan.delist.room.TaskDisplay

private const val ITEM_VIEW_TYPE_DETAIL = 0
private const val ITEM_VIEW_TYPE_HEADER = 1
private const val ITEM_VIEW_TYPE_COMPLETED_TASK = 2


class CollectionDetailAdapter(private val onClickListener: OnClickListener) :
    ListAdapter<CollectionDetailListItem, RecyclerView.ViewHolder>(
        CollectionDetailListItemDiffCallback()
    ) {

    interface OnClickListener {
        fun onCreateTask()
        fun onCompleteTask()
        fun toggleCompletedShown()
        fun toggleIncompleteShown()
    }

    class ListItemCompletedTaskViewHolder(private val binding: ListItemCompletedTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CollectionDetailListItem.Task) {
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

    class ListItemHeaderViewHolder(private val binding: ListItemCompletedHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(@StringRes title: Int, expanded: Boolean, toggleExpanded: () -> Unit) {
            val context = binding.root.context
            binding.apply {
                headerTitle.text = context.getString(title)
                collapseButton.setOnClickListener { toggleExpanded() }
                collapseButton.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        context.resources,
                        if (expanded) R.drawable.ic_baseline_arrow_up_36
                        else R.drawable.ic_baseline_arrow_down_36,
                        context.theme
                    )
                )
            }

        }

        companion object {
            fun from(parent: ViewGroup): ListItemHeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemCompletedHeaderBinding.inflate(layoutInflater, parent, false)
                return ListItemHeaderViewHolder(binding)
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
                onClickListener.onCreateTask()
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
                item as CollectionDetailListItem.Task
            )
            is ListItemHeaderViewHolder -> {
                when (item) {
                    is CollectionDetailListItem.CompletedHeader -> {
                        holder.bind(
                            R.string.completed_tasks,
                            item.expanded,
                            onClickListener::toggleCompletedShown
                        )
                    }
                    is CollectionDetailListItem.IncompleteHeader -> {
                        holder.bind(
                            R.string.incomplete_tasks,
                            item.expanded,
                            onClickListener::toggleIncompleteShown
                        )
                    }
                    else -> {
                        /* no-op */
                    }
                }
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
            ITEM_VIEW_TYPE_HEADER -> ListItemHeaderViewHolder.from(parent)
            ITEM_VIEW_TYPE_COMPLETED_TASK -> ListItemCompletedTaskViewHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is CollectionDetailListItem.Task -> ITEM_VIEW_TYPE_COMPLETED_TASK
            is CollectionDetailListItem.DetailItem -> ITEM_VIEW_TYPE_DETAIL
            is CollectionDetailListItem.CompletedHeader -> ITEM_VIEW_TYPE_HEADER
            is CollectionDetailListItem.IncompleteHeader -> ITEM_VIEW_TYPE_HEADER
        }
    }

    fun submit(
        taskString: String?,
        completedTasks: List<TaskDisplay>,
        incompleteTasks: List<TaskDisplay>,
        showCompleted: Boolean,
        showIncomplete: Boolean
    ) {
        val list = mutableListOf<CollectionDetailListItem>(
            CollectionDetailListItem.DetailItem(taskString),
        )
        list += listOf(CollectionDetailListItem.CompletedHeader(showCompleted))
        if (showCompleted) list += completedTasks.map { CollectionDetailListItem.Task(it) }
        list += listOf(CollectionDetailListItem.IncompleteHeader(showIncomplete))
        if (showIncomplete) list += incompleteTasks.map { CollectionDetailListItem.Task(it) }

        submitList(list)
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
            is CollectionDetailListItem.Task -> oldItem.content == (newItem as CollectionDetailListItem.Task).content
            is CollectionDetailListItem.DetailItem -> oldItem.currentTask == (newItem as CollectionDetailListItem.DetailItem).currentTask
            is CollectionDetailListItem.CompletedHeader -> oldItem.expanded == (newItem as CollectionDetailListItem.CompletedHeader).expanded
            is CollectionDetailListItem.IncompleteHeader -> oldItem.expanded == (newItem as CollectionDetailListItem.IncompleteHeader).expanded
        }
    }
}

sealed class CollectionDetailListItem {
    abstract val id: Long

    class DetailItem(val currentTask: String?) : CollectionDetailListItem() {
        override val id = -1L
    }

    data class CompletedHeader(val expanded: Boolean) : CollectionDetailListItem() {
        override val id = -2L
    }

    data class IncompleteHeader(val expanded: Boolean) : CollectionDetailListItem() {
        override val id = -3L
    }

    class Task(data: TaskDisplay) : CollectionDetailListItem() {
        override val id = data.id
        val content = data.content
    }
}