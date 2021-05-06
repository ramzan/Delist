package ca.ramzan.delist.screens.collection_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import ca.ramzan.delist.R
import ca.ramzan.delist.common.safeNavigate
import ca.ramzan.delist.databinding.FragmentCollectionDetailBinding
import ca.ramzan.delist.screens.BaseFragment
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CollectionDetailFragment : BaseFragment<FragmentCollectionDetailBinding>() {

    private lateinit var viewModel: CollectionDetailViewModel

    override fun onStart() {
        super.onStart()
        setUpBottomBar()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mutableBinding = FragmentCollectionDetailBinding.inflate(inflater)

        return binding.root
    }

    private fun setUpBottomBar() {
        requireActivity().run {
            findViewById<BottomAppBar>(R.id.bottom_app_bar)?.run {
                visibility = View.VISIBLE
                fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_END
            }
            findViewById<FloatingActionButton>(R.id.fab)?.run {
                setImageResource(R.drawable.ic_baseline_edit_24)
                setOnClickListener {
                    findNavController(R.id.nav_host_fragment).safeNavigate(
                        CollectionDetailFragmentDirections.actionCollectionDetailFragmentToCollectionEditorFragment()
                            .setCollectionId(requireArguments().getLong("collectionId"))
                    )
                }
                show()
            }
        }
    }
}