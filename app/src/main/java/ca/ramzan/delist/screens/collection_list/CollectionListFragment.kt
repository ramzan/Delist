package ca.ramzan.delist.screens.collection_list

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView
import ca.ramzan.delist.R
import ca.ramzan.delist.common.safeNavigate
import ca.ramzan.delist.common.typeToColor
import ca.ramzan.delist.databinding.FragmentCollectionListBinding
import ca.ramzan.delist.screens.BaseFragment
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

const val PREF_COLLECTION_ORDER_KEY = "PREF_COLLECTION_ORDER_KEY"
const val PREF_COLLECTION_ORDER_MANUAL = "PREF_COLLECTION_ORDER_MANUAL"
const val PREF_COLLECTION_ORDER_ASC = "PREF_COLLECTION_ORDER_ASC"
const val PREF_COLLECTION_ORDER_DESC = "PREF_COLLECTION_ORDER_DESC"

@AndroidEntryPoint
class CollectionListFragment : BaseFragment<FragmentCollectionListBinding>() {

    @Inject
    lateinit var prefs: SharedPreferences

    private val viewModel: CollectionListViewModel by viewModels()

    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun onStart() {
        super.onStart()
        setUpBottomBar()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mutableBinding = FragmentCollectionListBinding.inflate(inflater)
        requireActivity().window.statusBarColor = resources.getColor(R.color.status_bar, null)

        val adapter = CollectionAdapter(
            ::goToCollection,
            ::completeTask,
            getString(R.string.empty_collection_message)
        )

        binding.collectionList.adapter = adapter

        itemTouchHelper = ItemTouchHelper(getItemTouchCallback(adapter))
        enableReordering(
            prefs.getString(
                PREF_COLLECTION_ORDER_KEY,
                PREF_COLLECTION_ORDER_MANUAL
            ) == PREF_COLLECTION_ORDER_MANUAL
        )

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.collections.collect { list ->
                adapter.submitList(list.map {
                    CollectionDisplay(
                        it.id,
                        it.name,
                        typeToColor(resources, it.color),
                        it.task
                    )
                })
            }
        }

        return binding.root
    }

    private fun goToCollection(collectionId: Long) {
        findNavController().safeNavigate(
            CollectionListFragmentDirections.actionCollectionListFragmentToCollectionDetailFragment(
                collectionId
            )
        )
    }

    private fun completeTask(collectionId: Long) {
        viewModel.completeTask(collectionId)
        Snackbar.make(
            requireView(),
            getString(R.string.complete_task_message),
            Snackbar.LENGTH_SHORT
        )
            .setAction(getString(R.string.undo)) {
                viewModel.undoCompleteTask(collectionId)
            }
            // setAnchorView causes a memory leak
            // https://github.com/material-components/material-components-android/issues/2042
            .setAnchorView(requireActivity().findViewById<FloatingActionButton>(R.id.fab))
            .show()
    }

    private fun setUpBottomBar() {
        requireActivity().run {
            findViewById<BottomAppBar>(R.id.bottom_app_bar)?.run {
                visibility = View.VISIBLE
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.manual_sort -> {
                            prefs.edit {
                                putString(PREF_COLLECTION_ORDER_KEY, PREF_COLLECTION_ORDER_MANUAL)
                            }
                            viewModel.getCollections()
                            enableReordering(true)
                            true
                        }
                        R.id.alpha_asc_sort -> {
                            prefs.edit {
                                putString(PREF_COLLECTION_ORDER_KEY, PREF_COLLECTION_ORDER_ASC)
                            }
                            viewModel.getCollections()
                            enableReordering(false)
                            true
                        }
                        R.id.alpha_desc_sort -> {
                            prefs.edit {
                                putString(PREF_COLLECTION_ORDER_KEY, PREF_COLLECTION_ORDER_DESC)
                            }
                            viewModel.getCollections()
                            enableReordering(false)
                            true
                        }
                        else -> false
                    }
                }
            }
            findViewById<FloatingActionButton>(R.id.fab)?.run {
                setImageResource(R.drawable.ic_baseline_add_24)
                setOnClickListener {
                    findNavController(R.id.nav_host_fragment).safeNavigate(
                        CollectionListFragmentDirections.actionCollectionListFragmentToCollectionEditorFragment()
                    )
                }
                show()
            }
        }
    }

    private fun enableReordering(enable: Boolean) {
        itemTouchHelper.attachToRecyclerView(if (enable) binding.collectionList else null)
    }

    private fun getItemTouchCallback(adapter: CollectionAdapter): ItemTouchHelper.SimpleCallback {
        return object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END,
            0
        ) {

            private var dragFrom = -1
            private var dragTo = -1

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val dragFlags =
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END
                val swipeFlags = 0
                return makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.layoutPosition
                val toPos = target.layoutPosition

                if (dragFrom == -1) {
                    dragFrom = fromPos
                }
                dragTo = toPos

                val newList = adapter.currentList.toMutableList()
                newList.add(toPos, newList.removeAt(fromPos))
                adapter.submitList(newList)
                return true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState != ACTION_STATE_IDLE) {
                    viewHolder?.itemView?.alpha = 0.5f
                    viewHolder?.itemView?.findViewById<TextView>(R.id.item_text)?.maxLines = 1
                }
                if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                    viewModel.moveItem(dragFrom, dragTo)
                }
                dragFrom = -1
                dragTo = -1
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
                viewHolder.itemView.findViewById<TextView>(R.id.item_text)?.maxLines = 3
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                /* no-op */
            }
        }
    }
}