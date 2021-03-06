package ca.ramzan.delist.screens.collection_detail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import ca.ramzan.delist.R
import ca.ramzan.delist.common.safeNavigate
import ca.ramzan.delist.common.typeToColor
import ca.ramzan.delist.databinding.FragmentCollectionDetailBinding
import ca.ramzan.delist.screens.BaseFragment
import ca.ramzan.delist.screens.dialogs.CONFIRMATION_RESULT
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

const val KEY_COLLECTION_DELETED = "KEY_COLLECTION_DELETED"
const val KEY_COMPLETED_DELETED = "KEY_COMPLETED_DELETED"

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
        setFragmentResultListener(TASK_INPUT_RESULT) { _, bundle ->
            bundle.getString(TASK_INPUT_TEXT)?.let {
                viewModel.addTasks(it)
                Snackbar.make(
                    binding.root,
                    getString(R.string.tasks_added_message),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
        setFragmentResultListener(CONFIRMATION_RESULT) { _, bundle ->
            if (bundle.getBoolean(KEY_COLLECTION_DELETED, false)) {
                viewModel.deleteCollection()
                goBackDeleted()
            } else if (bundle.getBoolean(KEY_COMPLETED_DELETED, false)) {
                viewModel.clearCompleted()
                Snackbar.make(
                    binding.root,
                    getString(R.string.completed_deleted_message),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setUpBinding(FragmentCollectionDetailBinding.inflate(inflater))

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

            override fun toggleCompletedShown() {
                viewModel.toggleCompletedShown()
            }

            override fun toggleIncompleteShown() {
                viewModel.toggleIncompleteShown()
            }
        })

        binding.completedTaskList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.state.collect { state ->
                when (state) {
                    DetailState.Deleted -> goBackDeleted()
                    is DetailState.Loaded -> {
                        binding.detailToolbar.apply {
                            title = state.collection.name
                            typeToColor(resources, state.collection.color).let { color ->
                                setBackgroundColor(color)
                                binding.root.setBackgroundColor(color)
                            }
                            setNavigationOnClickListener {
                                findNavController().popBackStack(R.id.collectionListFragment, false)
                            }
                            if (state.collection.archived) {
                                menu.findItem(R.id.archive).isVisible = false
                                menu.findItem(R.id.unarchive).isVisible = true
                            } else {
                                menu.findItem(R.id.archive).isVisible = true
                                menu.findItem(R.id.unarchive).isVisible = false
                            }
                            setOnMenuItemClickListener { menuItem ->
                                when (menuItem.itemId) {
                                    R.id.archive -> {
                                        viewModel.archiveCollection(true)
                                        Snackbar.make(
                                            binding.root,
                                            context.getString(R.string.collection_archived_message),
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                        true
                                    }
                                    R.id.unarchive -> {
                                        viewModel.archiveCollection(false)
                                        Snackbar.make(
                                            binding.root,
                                            context.getString(R.string.collection_unarchived_message),
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                        true
                                    }
                                    R.id.delete_collection -> {
                                        showDeleteCollectionDialog()
                                        true
                                    }
                                    R.id.delete_completed -> {
                                        showDeleteCompletedDialog()
                                        true
                                    }
                                    R.id.edit -> {
                                        findNavController().safeNavigate(
                                            CollectionDetailFragmentDirections.actionCollectionDetailFragmentToCollectionEditorFragment()
                                                .setCollectionId(requireArguments().getLong("collectionId"))
                                        )
                                        true
                                    }
                                    R.id.export -> {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val exportFile = File(
                                                requireContext().externalCacheDir,
                                                "${state.collection.name}.txt"
                                            )
                                            exportFile.writeText("")
                                            viewModel.exportList().forEach {
                                                exportFile.appendText(
                                                    "- [${if (it.timeCompleted == null) ' ' else 'x'}] ${it.content}\n"
                                                )
                                            }
                                            val contentUri = FileProvider.getUriForFile(
                                                requireContext(),
                                                "ca.ramzan.fileprovider",
                                                exportFile
                                            )
                                            val shareIntent: Intent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_STREAM, contentUri)
                                                type = "text/plain"
                                            }
                                            startActivity(
                                                Intent.createChooser(
                                                    shareIntent,
                                                    getString(R.string.export_sharesheet_title)
                                                )
                                            )
                                        }
                                        true
                                    }
                                    else -> false
                                }
                            }
                        }

                        adapter.submit(
                            state.collection.task,
                            state.completedTasks,
                            state.incompleteTasks,
                            state.showCompleted,
                            state.showIncomplete
                        )

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

    private fun goBackDeleted() {
        findNavController().safeNavigate(
            CollectionDetailFragmentDirections.actionCollectionDetailFragmentToCollectionListFragment()
                .apply { collectionDeleted = true }
        )
    }

    private fun showDeleteCollectionDialog() {
        findNavController().safeNavigate(
            CollectionDetailFragmentDirections.actionCollectionDetailFragmentToConfirmationDialog(
                R.string.delete_collection_dialog_title,
                R.string.message_action_cannot_be_undone,
                R.string.delete,
                KEY_COLLECTION_DELETED
            )
        )
    }

    private fun showDeleteCompletedDialog() {
        findNavController().safeNavigate(
            CollectionDetailFragmentDirections.actionCollectionDetailFragmentToConfirmationDialog(
                R.string.delete_completed_dialog_title,
                R.string.message_action_cannot_be_undone,
                R.string.delete,
                KEY_COMPLETED_DELETED
            )
        )
    }


    private fun showTaskInputDialog() {
        findNavController().safeNavigate(
            CollectionDetailFragmentDirections.actionCollectionDetailFragmentToTaskInputDialogFragment()
        )
    }
}