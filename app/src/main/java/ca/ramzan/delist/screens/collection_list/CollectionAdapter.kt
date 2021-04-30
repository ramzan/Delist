package ca.ramzan.delist.screens.collection_list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.ramzan.delist.databinding.ListItemCollectionBinding
import ca.ramzan.delist.room.CollectionDisplay

class CollectionAdapter(private val onClickListener: OnClickListener) :
    ListAdapter<CollectionDisplay, CollectionAdapter.ListItemCollectionViewHolder>(
        CollectionDiffCallback()
    ) {

    interface OnClickListener

    class ListItemCollectionViewHolder(private val binding: ListItemCollectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CollectionDisplay, onClickListener: OnClickListener) {
            binding.collectionName.text = item.name
            binding.itemText.text = item.item
            binding.completeItemButton.setOnClickListener {
            }
        }

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
        holder.bind(item, onClickListener)
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