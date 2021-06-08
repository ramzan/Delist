package ca.ramzan.delist.screens.backup_restore

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import ca.ramzan.delist.R
import ca.ramzan.delist.common.MainActivity
import ca.ramzan.delist.databinding.FragmentBackupRestoreBinding
import ca.ramzan.delist.room.CollectionDatabase
import ca.ramzan.delist.screens.BaseFragment
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

private const val EXPORT_TITLE = "DelistBackup.db"
private const val IMPORT_MIMETYPE = "application/octet-stream"
const val IMPORT_SUCCESS = "IMPORT_SUCCESS"

class BackupRestoreFragment : BaseFragment<FragmentBackupRestoreBinding>() {

    private val importDb =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            val success = CollectionDatabase.copyFrom(requireContext(), uri)
            if (success) {
                startActivity(Intent(requireActivity(), MainActivity::class.java).apply {
                    putExtra(IMPORT_SUCCESS, true)
                })
                requireActivity().finish()
            } else {
                Snackbar.make(
                    requireView(),
                    getString(R.string.import_db_error_message),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

    private val exportDb =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
            if (uri != null) CollectionDatabase.copyTo(requireContext(), uri)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mutableBinding = FragmentBackupRestoreBinding.inflate(inflater)

        binding.editorToolbar.setNavigationOnClickListener {
            findNavController().popBackStack(
                R.id.collectionListFragment,
                false
            )
        }
        binding.importButton.setOnClickListener { importDb.launch(arrayOf(IMPORT_MIMETYPE)) }
        binding.exportButton.setOnClickListener { exportDb.launch(EXPORT_TITLE) }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        setUpBottomBar()
    }

    private fun setUpBottomBar() {
        requireActivity().run {
            findViewById<FloatingActionButton>(R.id.fab)?.hide()
            findViewById<BottomAppBar>(R.id.bottom_app_bar)?.visibility = View.INVISIBLE
        }
    }
}