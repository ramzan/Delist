package ca.ramzan.delist.screens.backup_restore

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider.getUriForFile
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import ca.ramzan.delist.R
import ca.ramzan.delist.common.MainActivity
import ca.ramzan.delist.databinding.FragmentBackupRestoreBinding
import ca.ramzan.delist.room.DB_NAME
import ca.ramzan.delist.screens.BaseFragment
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

private const val DB_MIMETYPE = "application/octet-stream"
const val IMPORT_SUCCESS = "IMPORT_SUCCESS"

class BackupRestoreFragment : BaseFragment<FragmentBackupRestoreBinding>() {

    private val viewModel: BackupRestoreViewModel by viewModels()

    private val dateString: String
        get() = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").run {
            format(Instant.now().atZone(ZoneId.systemDefault()))
        }

    private val importDb =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            viewModel.import(requireActivity().applicationContext, uri)
        }

    private fun exportDb() {
        val exportName = "Delist Backup $dateString.db"
        val dbFile = File(requireContext().externalCacheDir, exportName)
        requireContext().getDatabasePath(DB_NAME).inputStream().copyTo(dbFile.outputStream())
        val contentUri = getUriForFile(requireContext(), "ca.ramzan.fileprovider", dbFile)
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, contentUri)
            type = DB_MIMETYPE
        }
        startActivity(
            Intent.createChooser(
                shareIntent,
                getString(R.string.export_sharesheet_title)
            )
        )
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
        binding.importButton.setOnClickListener { importDb.launch(arrayOf(DB_MIMETYPE)) }
        binding.exportButton.setOnClickListener { exportDb() }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.state.collect { state ->
                when (state) {
                    BackupRestoreState.Idle -> {
                        /* no-op */
                    }
                    BackupRestoreState.ImportFailed -> {
                        Snackbar.make(
                            requireView(),
                            getString(R.string.import_db_error_message),
                            Snackbar.LENGTH_SHORT
                        ).show()
                        viewModel.onErrorShown()
                    }
                    BackupRestoreState.ImportSuccess -> {
                        startActivity(Intent(requireActivity(), MainActivity::class.java).apply {
                            putExtra(IMPORT_SUCCESS, true)
                        })
                        requireActivity().finish()
                    }
                    BackupRestoreState.Processing -> {
                        /* no-op */
                    }
                }
            }
        }

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