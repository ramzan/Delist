package ca.ramzan.delist.common

import androidx.navigation.NavController
import androidx.navigation.NavDirections

fun NavController.safeNavigate(directions: NavDirections) {
    currentDestination?.getAction(directions.actionId)?.let {
        navigate(directions)
    }
}