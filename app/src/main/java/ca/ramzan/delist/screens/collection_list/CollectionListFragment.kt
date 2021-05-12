package ca.ramzan.delist.screens.collection_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import ca.ramzan.delist.R
import ca.ramzan.delist.common.safeNavigate
import ca.ramzan.delist.common.typeToColor
import ca.ramzan.delist.databinding.FragmentCollectionListBinding
import ca.ramzan.delist.screens.BaseFragment
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class CollectionListFragment : BaseFragment<FragmentCollectionListBinding>() {

    private val viewModel: CollectionListViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        setUpBottomBar()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mutableBinding = FragmentCollectionListBinding.inflate(inflater)
        requireActivity().window.statusBarColor = resources.getColor(R.color.primary, null)

        val adapter = CollectionAdapter(::goToCollection, ::completeTask)

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

        binding.collectionList.adapter = adapter

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
    }

    private fun setUpBottomBar() {
        requireActivity().run {
            findViewById<BottomAppBar>(R.id.bottom_app_bar)?.run {
                visibility = View.VISIBLE
                setFabAlignmentModeAndReplaceMenu(
                    BottomAppBar.FAB_ALIGNMENT_MODE_CENTER,
                    R.menu.list_menu
                )
                navigationIcon =
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_menu_24)
                setOnMenuItemClickListener(null)
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
}