package ca.ramzan.delist.screens.backup_restore

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.ramzan.delist.room.CollectionDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
//    private val dao: CollectionDatabaseDao
) : ViewModel() {

    fun import(context: Context, uri: Uri) {
        viewModelScope.launch {
            state.emit(BackupRestoreState.Processing)
            CoroutineScope(Dispatchers.IO).launch {
                state.emit(
                    if (CollectionDatabase.copyFrom(context, uri)) {
                        BackupRestoreState.ImportSuccess
                    } else BackupRestoreState.ImportFailed
                )
            }
        }
    }

    fun onErrorShown() {
        viewModelScope.launch { state.emit(BackupRestoreState.Idle) }
    }

    val state = MutableStateFlow<BackupRestoreState>(BackupRestoreState.Idle)

}

sealed class BackupRestoreState {

    object Idle : BackupRestoreState()
    object Processing : BackupRestoreState()
    object ImportFailed : BackupRestoreState()
    object ImportSuccess : BackupRestoreState()
}
