package ca.ramzan.delist.screens.collection_list

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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
import ca.ramzan.delist.common.safeNavigate
import ca.ramzan.delist.common.typeToColor
import ca.ramzan.delist.databinding.FragmentCollectionListBinding
import ca.ramzan.delist.screens.BaseFragment
import ca.ramzan.delist.screens.backup_restore.IMPORT_SUCCESS
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.math.MathUtils.lerp
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

const val PREF_COLLECTION_HIDE_ARCHIVED = "PREF_COLLECTION_HIDE_ARCHIVED"
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
            viewModel.state.collect { state ->
                when (state) {
                    is ListState.Loading -> {
                        binding.listProgressBar.visibility = View.VISIBLE
                        binding.noCollectionsMessage.visibility = View.GONE
                        binding.collectionList.visibility = View.GONE
                    }
                    is ListState.NoCollections -> {
                        binding.listProgressBar.visibility = View.GONE
                        binding.noCollectionsMessage.visibility = View.VISIBLE
                        binding.collectionList.visibility = View.GONE
                    }
                    is ListState.Loaded -> {
                        adapter.submitList(state.collections.map {
                            CollectionDisplay(
                                it.id,
                                it.name,
                                typeToColor(resources, it.color),
                                it.task
                            )
                        })
                        binding.listProgressBar.visibility = View.GONE
                        binding.noCollectionsMessage.visibility = View.GONE
                        binding.collectionList.visibility = View.VISIBLE
                    }
                }
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        itemTouchHelper.attachToRecyclerView(null)
        super.onDestroyView()
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
                prefs.getString(PREF_COLLECTION_ORDER_KEY, PREF_COLLECTION_ORDER_MANUAL).run {
                    when (this) {
                        PREF_COLLECTION_ORDER_MANUAL -> menu.findItem(R.id.manual_sort).isChecked =
                            true
                        PREF_COLLECTION_ORDER_ASC -> menu.findItem(R.id.alpha_asc_sort).isChecked =
                            true
                        PREF_COLLECTION_ORDER_DESC -> menu.findItem(R.id.alpha_desc_sort).isChecked =
                            true
                    }
                }

                menu.findItem(R.id.hide_archived).isChecked =
                    prefs.getBoolean(PREF_COLLECTION_HIDE_ARCHIVED, false)

                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.hide_archived -> {
                            it.isChecked = !it.isChecked
                            prefs.edit {
                                putBoolean(PREF_COLLECTION_HIDE_ARCHIVED, it.isChecked)
                            }
                            viewModel.getCollections()
                            true
                        }
                        R.id.manual_sort -> {
                            prefs.edit {
                                putString(PREF_COLLECTION_ORDER_KEY, PREF_COLLECTION_ORDER_MANUAL)
                            }
                            viewModel.getCollections()
                            enableReordering(true)
                            it.isChecked = true
                            true
                        }
                        R.id.alpha_asc_sort -> {
                            prefs.edit {
                                putString(PREF_COLLECTION_ORDER_KEY, PREF_COLLECTION_ORDER_ASC)
                            }
                            viewModel.getCollections()
                            enableReordering(false)
                            it.isChecked = true
                            true
                        }
                        R.id.alpha_desc_sort -> {
                            prefs.edit {
                                putString(PREF_COLLECTION_ORDER_KEY, PREF_COLLECTION_ORDER_DESC)
                            }
                            viewModel.getCollections()
                            enableReordering(false)
                            it.isChecked = true
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
                        R.id.theme -> showThemeSelector()
                        R.id.import_export -> goToBackupRestoreFragment()
                    }
//                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    true
                }

                binding.scrim.setOnClickListener {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }

                bottomSheetBehavior.addBottomSheetCallback(object :
                    BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        val baseColor = Color.BLACK
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
            if (intent.getBooleanExtra(IMPORT_SUCCESS, false)) {
                intent.removeExtra(IMPORT_SUCCESS)
                Snackbar.make(
                    requireView(),
                    getString(R.string.import_db_success_message),
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(fab)
                    .show()
            }
        }
    }

    private fun goToBackupRestoreFragment() {
        findNavController().safeNavigate(
            CollectionListFragmentDirections.actionCollectionListFragmentToBackupRestoreFragment()
        )
    }

    private fun showThemeSelector() {
        findNavController().safeNavigate(
            CollectionListFragmentDirections.actionCollectionListFragmentToThemeSelectorDialog()
        )
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