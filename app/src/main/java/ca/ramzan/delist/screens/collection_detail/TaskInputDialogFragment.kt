package ca.ramzan.delist.screens.collection_detail

import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import ca.ramzan.delist.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

const val TASK_INPUT_RESULT = "TASK_INPUT_RESULT"
const val TASK_INPUT_TEXT = "TASK_INPUT_TEXT"

class TaskInputDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layout = layoutInflater.inflate(R.layout.task_input_layout, null)
        val input = layout.findViewById<EditText>(R.id.new_task_input)

        return MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle(getString(R.string.task_input_dialog_title))
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                setFragmentResult(
                    TASK_INPUT_RESULT,
                    bundleOf(TASK_INPUT_TEXT to input.text.toString())
                )
            }
            .setNegativeButton(getString(R.string.dialog_negative_button_label)) { _, _ ->
                /* no-op */
            }
            .setView(layout)
            .create().apply {
                window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                setOnShowListener {
                    input.requestFocus()
                }
            }
    }
}