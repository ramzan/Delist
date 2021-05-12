package ca.ramzan.delist.screens.collection_list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.ramzan.delist.databinding.ListItemCollectionBinding

class CollectionAdapter(
    private val onCollectionTap: (Long) -> Unit,
    private val onCompleteTask: (Long) -> Unit
) :
    ListAdapter<CollectionDisplay, CollectionAdapter.ListItemCollectionViewHolder>(
        CollectionDiffCallback()
    ) {

    class ListItemCollectionViewHolder(val binding: ListItemCollectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): ListItemCollectionViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemCollectionBinding.inflate(layoutInflater, parent, false)
                return ListItemCollectionViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: ListItemCollectionViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.run {
            collectionName.text = item.name
            itemText.text = item.item
            root.background.colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    item.color,
                    BlendModeCompat.SRC_ATOP
                )
            root.setOnClickListener {
                onCollectionTap(item.id)
            }
            completeTaskButton.setOnClickListener {
                onCompleteTask(item.id)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListItemCollectionViewHolder {
        return ListItemCollectionViewHolder.from(parent)
    }

}

class CollectionDiffCallback : DiffUtil.ItemCallback<CollectionDisplay>() {
    override fun areItemsTheSame(oldItem: CollectionDisplay, newItem: CollectionDisplay): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: CollectionDisplay,
        newItem: CollectionDisplay
    ): Boolean {
        return oldItem.name == newItem.name &&
                oldItem.item == newItem.item &&
                oldItem.color == newItem.color
    }
}

class CollectionDisplay(val id: Long, val name: String, val color: Int, val item: String?)