package ca.ramzan.delist.screens.color_picker

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.RecyclerView
import ca.ramzan.delist.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

const val COLOR_PICKER_RESULT = "COLOR_PICKER_RESULT"
const val COLOR = "COLOR"

class ColorPickerDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layout = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        layout.findViewById<RecyclerView>(R.id.color_grid).adapter =
            ColorAdapter(resources.getIntArray(R.array.palette)) { color ->
                setFragmentResult(COLOR_PICKER_RESULT, bundleOf(COLOR to color))
                dismiss()
            }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change color")
            .setView(layout)
            .create()
    }


}