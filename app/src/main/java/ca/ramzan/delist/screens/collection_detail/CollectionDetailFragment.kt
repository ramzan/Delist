package ca.ramzan.delist.screens.collection_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mutableBinding = FragmentCollectionDetailBinding.inflate(inflater)

        val adapter = CompletedTaskAdapter()

        binding.completedTaskList.adapter = adapter

        binding.createTaskButton.setOnClickListener {
            viewModel.addTasks(binding.newTaskInput.text.toString())
            binding.newTaskInput.text = null
        }

        binding.completeTaskButton.setOnClickListener {
            viewModel.completeTask()
        }

        binding.clearCompletedButton.setOnClickListener {
            viewModel.clearCompleted()
        }

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
                                requireActivity().window.statusBarColor = color
                            }
                            setNavigationOnClickListener {
                                findNavController().popBackStack(R.id.collectionListFragment, false)
                            }
                            setOnMenuItemClickListener { menuItem ->
                                when (menuItem.itemId) {
                                    R.id.delete -> {
                                        viewModel.deleteCollection()
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
                        binding.root.setBackgroundColor(
                            typeToColor(
                                resources,
                                state.collection.color
                            )
                        )
                        binding.currentTask.text =
                            state.collection.task ?: getString(R.string.empty_collection_message)
                        adapter.submitList(state.completedTasks)
                    }
                    DetailState.Loading -> {
                    }
                }
            }
        }

        return binding.root
    }

    private fun setUpBottomBar() {
        requireActivity().run {
            findViewById<FloatingActionButton>(R.id.fab)?.hide()
            findViewById<BottomAppBar>(R.id.bottom_app_bar)?.visibility = View.INVISIBLE
        }
    }
}