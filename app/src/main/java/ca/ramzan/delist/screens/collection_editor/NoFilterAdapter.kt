package ca.ramzan.delist.screens.collection_editor

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

/*
 * Used to prevent AutoCompleteTextView from filtering options
 */
class NoFilterAdapter(context: Context, resource: Int, objects: Array<String>) : ArrayAdapter<Any?>(
    context,
    resource,
    objects
) {

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults? {
                return null
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {}
        }
    }
}
