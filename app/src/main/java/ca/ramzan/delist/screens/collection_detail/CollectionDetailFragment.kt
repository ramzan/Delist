package ca.ramzan.delist.screens.collection_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import ca.ramzan.delist.R
import ca.ramzan.delist.common.safeNavigate
import ca.ramzan.delist.common.typeToColor
import ca.ramzan.delist.databinding.FragmentCollectionDetailBinding
import ca.ramzan.delist.screens.BaseFragment
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@AndroidEntryPoint
class CollectionDetailFragment : BaseFragment<FragmentCollectionDetailBinding>() {

    @Inject
    lateinit var factory: CollectionDetailViewModel.Factory

    private val viewModel: CollectionDetailViewModel by viewModels {
        CollectionDetailViewModel.provideFactory(
            factory,
            requireArguments().getLong("collectionId")
        )
    }

    override fun onStart() {
        super.onStart()
        setUpBottomBar()
        setFragmentResultListener(TASK_INPUT_RESULT) { _, bundle ->
            bundle.getString(TASK_INPUT_TEXT)?.let {
                viewModel.addTasks(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mutableBinding = FragmentCollectionDetailBinding.inflate(inflater)

        val adapter = CollectionDetailAdapter(object : CollectionDetailAdapter.OnClickListener {
            override fun onCreateTask() {
                showTaskInputDialog()
            }

            override fun onCompleteTask() {
                viewModel.completeTask()
                Snackbar.make(
                    requireView(),
                    getString(R.string.complete_task_message),
                    Snackbar.LENGTH_SHORT
                )
                    .setAction(getString(R.string.undo)) {
                        viewModel.undoCompleteTask()
                    }
                    .show()
            }
        })

        binding.completedTaskList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.state.collect { state ->
                when (state) {
                    DetailState.Deleted -> findNavController().popBackStack(
                        R.id.collectionListFragment,
                        false
                    )
                    is DetailState.Loaded -> {
                        binding.detailToolbar.apply {
                            title = state.collection.name
                            typeToColor(resources, state.collection.color).let { color ->
                                setBackgroundColor(color)
                                binding.root.setBackgroundColor(color)
                                requireActivity().window.statusBarColor = color
                            }
                            setNavigationOnClickListener {
                                findNavController().popBackStack(R.id.collectionListFragment, false)
                            }
                            setOnMenuItemClickListener { menuItem ->
                                when (menuItem.itemId) {
                                    R.id.delete_collection -> {
                                        viewModel.deleteCollection()
                                        true
                                    }
                                    R.id.delete_completed -> {
                                        viewModel.clearCompleted()
                                        true
                                    }
                                    R.id.edit -> {
                                        findNavController().safeNavigate(
                                            CollectionDetailFragmentDirections.actionCollectionDetailFragmentToCollectionEditorFragment()
                                                .setCollectionId(requireArguments().getLong("collectionId"))
                                        )
                                        true
                                    }
                                    else -> false
                                }
                            }
                        }

                        if (state.completedTasks.isEmpty()) {
                            adapter.submitNoCompleted(state.collection.task)
                        } else {
                            adapter.submitWithCompleted(state.collection.task, state.completedTasks)
                        }

//                        binding.completedTaskListHeader.text = resources.getQuantityString(
//                            R.plurals.n_completed_tasks,
//                            state.completedTasks.size,
//                            state.completedTasks.size
//                        )
//                        binding.completedTaskGroup.visibility =
//                            if (state.completedTasks.isEmpty()) View.GONE else View.VISIBLE

                        binding.detailProgressBar.visibility = View.GONE
                        binding.completedTaskList.visibility = View.VISIBLE
                    }
                    DetailState.Loading -> {
                        binding.detailProgressBar.visibility = View.VISIBLE
                        binding.completedTaskList.visibility = View.GONE
                    }
                }
            }
        }

        return binding.root
    }

    private fun showTaskInputDialog() {
        findNavController().safeNavigate(
            CollectionDetailFragmentDirections.actionCollectionDetailFragmentToTaskInputDialogFragment()
        )
    }

    private fun setUpBottomBar() {
        requireActivity().run {
            findViewById<FloatingActionButton>(R.id.fab)?.hide()
            findViewById<BottomAppBar>(R.id.bottom_app_bar)?.visibility = View.INVISIBLE
        }
    }
}