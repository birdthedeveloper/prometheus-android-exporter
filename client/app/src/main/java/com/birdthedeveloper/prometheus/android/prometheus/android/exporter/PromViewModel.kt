package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PromUiState(
    val tabIndex : Int = 0
)

val TAG : String = "PROMVIEWMODEL"

class PromViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PromUiState())
    val uiState : StateFlow<PromUiState> = _uiState.asStateFlow()

    init {
        //TODO implement this
        Log.v(TAG, "initializing promviewmodel")
    }

    fun updateTabIndex(index : Int){
        _uiState.update {current ->
            current.copy(
                tabIndex =  index
            )
        }
    }

}