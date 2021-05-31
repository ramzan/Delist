package ca.ramzan.delist.screens.collection_list

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView
import ca.ramzan.delist.R
import ca.ramzan.delist.common.MainActivity
import ca.ramzan.delist.common.safeNavigate
import ca.ramzan.delist.common.typeToColor
import ca.ramzan.delist.databinding.FragmentCollectionListBinding
import ca.ramzan.delist.room.CollectionDatabase
import ca.ramzan.delist.screens.BaseFragment
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.math.MathUtils.lerp
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
            val bottomBar = findViewById<BottomAppBar>(R.id.bottom_app_bar) ?: return
            val fab = findViewById<FloatingActionButton>(R.id.fab) ?: return
            bottomBar.apply {
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
                val bottomSheetBehavior = BottomSheetBehavior.from(binding.navView)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

                setNavigationOnClickListener {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }

                binding.navView.setNavigationItemSelectedListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.about -> onAboutClicked()
                        R.id.import_data -> importDb()
                        R.id.export_data -> exportDb()
                    }
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    true
                }

                binding.scrim.setOnClickListener {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }

                bottomSheetBehavior.addBottomSheetCallback(object :
                    BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        val baseColor = Color.BLACK
                        // 60% opacity
                        val baseAlpha =
                            ResourcesCompat.getFloat(resources, R.dimen.material_emphasis_medium)
                        // Map slideOffset from [-1.0, 1.0] to [0.0, 1.0]
                        val offset = (slideOffset - (-1f)) / (1f - (-1f)) * (1f - 0f) + 0f
                        val alpha = lerp(0f, 255f, offset * baseAlpha).toInt()
                        val color =
                            Color.argb(alpha, baseColor.red, baseColor.green, baseColor.blue)
                        binding.scrim.setBackgroundColor(color)
                    }

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        when (newState) {
                            BottomSheetBehavior.STATE_HIDDEN -> {
                                binding.scrim.visibility = View.GONE
                                bottomBar.visibility = View.VISIBLE
                                fab.show()
                            }
                            else -> {
                                binding.scrim.visibility = View.VISIBLE
                                bottomBar.visibility = View.GONE
                                fab.hide()
                            }
                        }
                    }
                })
            }
            fab.apply {
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

    private fun importDb() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/octet-stream")

            startActivityForResult(intent, REQUEST_IMPORT)
        } catch (t: Throwable) {
            Log.e(TAG, "Exception obtaining document for import", t)
        }

    }

    private fun exportDb() {
        try {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/octet-stream")
                .putExtra(Intent.EXTRA_TITLE, DEFAULT_EXPORT_TITLE)

            startActivityForResult(intent, REQUEST_EXPORT)
        } catch (t: Throwable) {
            Log.e(TAG, "Exception obtaining document for export", t)
        }

    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        when (requestCode) {
            REQUEST_EXPORT -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri -> CollectionDatabase.copyTo(requireContext(), uri) }
                }
            }
            REQUEST_IMPORT -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri -> CollectionDatabase.copyFrom(requireContext(), uri) }
                    startActivity(Intent(requireActivity(), MainActivity::class.java))
                    requireActivity().finish()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }


    private fun onAboutClicked() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ramzan/Delist")))
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

private const val DEFAULT_EXPORT_TITLE = "DelistBackup.db"
private const val REQUEST_EXPORT = 1337
private const val REQUEST_IMPORT = 1338
private const val TAG = "Delist Backup/Restore"
