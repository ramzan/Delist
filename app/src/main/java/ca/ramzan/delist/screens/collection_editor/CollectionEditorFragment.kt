package ca.ramzan.delist.screens.collection_editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import ca.ramzan.delist.R
import ca.ramzan.delist.common.colorToType
import ca.ramzan.delist.common.hideKeyboard
import ca.ramzan.delist.common.safeNavigate
import ca.ramzan.delist.common.typeToColor
import ca.ramzan.delist.databinding.FragmentCollectionEditorBinding
import ca.ramzan.delist.room.CollectionType
import ca.ramzan.delist.screens.BaseFragment
import ca.ramzan.delist.screens.color_picker.COLOR
import ca.ramzan.delist.screens.color_picker.COLOR_PICKER_RESULT
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@AndroidEntryPoint
class CollectionEditorFragment : BaseFragment<FragmentCollectionEditorBinding>() {

    @Inject
    lateinit var factory: CollectionEditorViewModel.Factory

    private val viewModel: CollectionEditorViewModel by viewModels {
        CollectionEditorViewModel.provideFactory(
            factory,
            requireArguments().getLong("collectionId")
        )
    }

    override fun onStart() {
        super.onStart()
        setUpBottomBar()
        setFragmentResultListener(COLOR_PICKER_RESULT) { _, bundle ->
            bundle.getInt(COLOR).let { color ->
                viewModel.updateColor(colorToType(resources, color))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mutableBinding = FragmentCollectionEditorBinding.inflate(inflater)

        binding.apply {
            editorToolbar.setNavigationOnClickListener {
                goBack()
            }

            editorToolbar.title = getString(
                if (requireArguments().getLong("collectionId") == 0L)
                    R.string.create_collection else R.string.edit_collection
            )

            editorToolbar.menu.getItem(0).setOnMenuItemClickListener {
                viewModel.saveCollection()
                it.isEnabled = false
                true
            }

            typeSelector.setAdapter(
                NoFilterAdapter(
                    requireContext(),
                    R.layout.list_item_type_selector,
                    resources.getStringArray(R.array.collection_type_array)
                )
            )

            colorButton.setOnClickListener {
                findNavController().safeNavigate(CollectionEditorFragmentDirections.actionCollectionEditorFragmentToColorPickerDialogFragment())
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                viewModel.state.collect { state ->
                    when (state) {
                        EditorState.Loading -> {
                            editorProgressbar.visibility = View.VISIBLE
                            editorLayout.visibility = View.GONE
                        }
                        is EditorState.Saved -> {
                            goToCollection(state.collectionId)
                        }
                        is EditorState.Loaded -> {
                            nameInput.setText(state.nameInputText)

                            nameInput.doOnTextChanged { text, _, _, _ ->
                                state.nameInputText = text.toString()
                            }

                            typeSelector.setText(getCollectionTypeName(state.collectionType), false)

                            typeSelector.setOnItemClickListener { _, _, _, _ ->
                                state.collectionType =
                                    getCollectionType(typeSelector.text.toString())
                            }

                            typeToColor(resources, state.color).let { color ->
                                editorToolbar.background.setTint(color)
                                requireActivity().window.statusBarColor = color
                                colorButton.background.colorFilter =
                                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                                        color,
                                        BlendModeCompat.SRC_ATOP
                                    )
                            }


                            editorProgressbar.visibility = View.GONE
                            editorLayout.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }

        return binding.root
    }

    private fun goBack() {
        requireContext().getSystemService(InputMethodManager::class.java)
            .hideKeyboard(requireView().windowToken)
        findNavController().popBackStack()
    }

    private fun goToCollection(collectionId: Long) {
        findNavController().safeNavigate(
            CollectionEditorFragmentDirections.actionCollectionEditorFragmentToCollectionDetailFragment(
                collectionId
            )
        )
    }

    private fun getCollectionTypeName(type: CollectionType): String {
        return when (type) {
            CollectionType.QUEUE -> getString(R.string.type_queue)
            CollectionType.STACK -> getString(R.string.type_stack)
            CollectionType.RANDOMIZER -> getString(R.string.type_randomizer)
        }
    }

    private fun getCollectionType(type: String): CollectionType {
        return when (type) {
            getString(R.string.type_queue) -> CollectionType.QUEUE
            getString(R.string.type_stack) -> CollectionType.STACK
            getString(R.string.type_randomizer) -> CollectionType.RANDOMIZER
            else -> throw Exception("Illegal type selection: type")
        }
    }

    private fun setUpBottomBar() {
        requireActivity().run {
            findViewById<FloatingActionButton>(R.id.fab)?.hide()
            findViewById<BottomAppBar>(R.id.bottom_app_bar)?.visibility = View.INVISIBLE
        }
    }
}