package ca.ramzan.delist.common

import android.content.res.Resources
import android.os.IBinder
import android.view.inputmethod.InputMethodManager
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import ca.ramzan.delist.R
import ca.ramzan.delist.room.CollectionColor

fun NavController.safeNavigate(directions: NavDirections) {
    currentDestination?.getAction(directions.actionId)?.let {
        navigate(directions)
    }
}

fun InputMethodManager.hideKeyboard(windowToken: IBinder) {
    this.hideSoftInputFromWindow(
        windowToken,
        InputMethodManager.HIDE_NOT_ALWAYS
    )
}

fun colorToType(resources: Resources, color: Int): CollectionColor {
    return when (color) {
        resources.getColor(R.color.plain, null) -> CollectionColor.PLAIN
        resources.getColor(R.color.red, null) -> CollectionColor.RED
        resources.getColor(R.color.orange, null) -> CollectionColor.ORANGE
        resources.getColor(R.color.yellow, null) -> CollectionColor.YELLOW
        resources.getColor(R.color.green, null) -> CollectionColor.GREEN
        resources.getColor(R.color.teal, null) -> CollectionColor.TEAL
        resources.getColor(R.color.blue, null) -> CollectionColor.BLUE
        resources.getColor(R.color.dark_blue, null) -> CollectionColor.DARK_BLUE
        resources.getColor(R.color.purple, null) -> CollectionColor.PURPLE
        resources.getColor(R.color.pink, null) -> CollectionColor.PINK
        resources.getColor(R.color.brown, null) -> CollectionColor.BROWN
        resources.getColor(R.color.grey, null) -> CollectionColor.GREY
        else -> throw Exception("Illegal color: $color")
    }
}

fun typeToColor(resources: Resources, color: CollectionColor): Int {
    return when (color) {
        CollectionColor.PLAIN -> resources.getColor(R.color.plain, null)
        CollectionColor.RED -> resources.getColor(R.color.red, null)
        CollectionColor.ORANGE -> resources.getColor(R.color.orange, null)
        CollectionColor.YELLOW -> resources.getColor(R.color.yellow, null)
        CollectionColor.GREEN -> resources.getColor(R.color.green, null)
        CollectionColor.TEAL -> resources.getColor(R.color.teal, null)
        CollectionColor.BLUE -> resources.getColor(R.color.blue, null)
        CollectionColor.DARK_BLUE -> resources.getColor(R.color.dark_blue, null)
        CollectionColor.PURPLE -> resources.getColor(R.color.purple, null)
        CollectionColor.PINK -> resources.getColor(R.color.pink, null)
        CollectionColor.BROWN -> resources.getColor(R.color.brown, null)
        CollectionColor.GREY -> resources.getColor(R.color.grey, null)
    }
}
