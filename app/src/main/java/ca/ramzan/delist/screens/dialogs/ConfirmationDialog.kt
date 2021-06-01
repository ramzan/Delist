package ca.ramzan.delist.screens.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import ca.ramzan.delist.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

const val CONFIRMATION_RESULT = "CONFIRMATION_RESULT"

class ConfirmationDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle(requireArguments().getInt("titleId"))
            .setMessage(requireArguments().getInt("messageId"))
            .setPositiveButton(requireArguments().getInt("positiveButtonMessage")) { _, _ ->
                setFragmentResult(
                    CONFIRMATION_RESULT,
                    bundleOf(requireArguments().getString("listenerKey", "") to true)
                )
            }
            .setNegativeButton(getString(R.string.dialog_negative_button_label)) { _, _ ->
                /* no-op */
            }
            .show()
    }


}