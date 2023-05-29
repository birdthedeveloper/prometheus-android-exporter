package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker.PushProxConfig
import com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker.PushProxWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ConfigFileState {
    LOADING,
    ERROR, // file not found or was not parsed
    MISSING,
    SUCCESS
}

data class PromUiState(
    val tabIndex : Int = 0,
    val serverTurnedOn : Boolean = false,
    val pushProxTurnedOn : Boolean = false,
    val serverPort : Int? = null, // if null, use default port
    val fqdn : String = "test.example.com",
    val pushProxURL : String = "143.42.59.63:8080",
    val configFileState : ConfigFileState = ConfigFileState.LOADING,
)

private val TAG : String = "PROMVIEWMODEL"

class PromViewModel(): ViewModel() {
    // constants
    private val DEFAULT_SERVER_PORT : Int = 10101 //TODO register with prometheus community
    private val PROM_UNIQUE_WORK : String = "prom_unique_job"


    private val _uiState = MutableStateFlow(PromUiState())
    val uiState : StateFlow<PromUiState> = _uiState.asStateFlow()

    private lateinit var getContext: () -> Context

    init {
        Log.v(TAG, "Checking for configuration file")
        viewModelScope.launch {
            //TODO check for configuration file
            delay(1000)
            _uiState.update { current ->
                current.copy(configFileState = ConfigFileState.MISSING)
            }
        }
    }

    fun getDefaultPort() : Int {
        return DEFAULT_SERVER_PORT
    }

    fun initializeWithApplicationContext(getContext : () -> Context){
        this.getContext = getContext
    }

    fun updateTabIndex(index : Int){
        _uiState.update {current ->
            current.copy(
                tabIndex =  index
            )
        }
    }

    private fun getPromServerPort() : Int{
        return if(_uiState.value.serverPort != null){
            _uiState.value.serverPort!!
        }else{
            DEFAULT_SERVER_PORT
        }
    }

    // if result is not null, it contains an error message
    fun turnServerOn() : String?{
        try{
            //TODO rewrite asap
//            prometheusServer.startInBackground(
//                PrometheusServerConfig(
//                    getPromServerPort(),
//                    ::performScrape
//                )
//            )
        }catch(e : Exception){
            Log.v(TAG, e.toString())
            return "Prometheus server failed!"
        }

        _uiState.update { current ->
            current.copy(
                serverTurnedOn = true
            )
        }
        return null;
    }

    fun turnServerOff(){
        //TODO implement
    }

    private fun validatePushProxSettings() : String? {
        val fqdn = _uiState.value.fqdn.trim().trim('\n')
        val url = _uiState.value.pushProxURL.trim().trim('\n')

        if( fqdn.isEmpty() ) return "Fully Qualified Domain Name cannot be empty!"
        if( url.isEmpty() ) return "PushProx URL cannot be empty!"

        return null
    }

    private fun launchPushProxUsingWorkManager(){
        val workManagerInstance = WorkManager.getInstance(getContext())

        // worker configuration
        val inputData : Data = PushProxConfig(
            pushProxFqdn = _uiState.value.fqdn,
            pushProxUrl = _uiState.value.pushProxURL,
        ).toData()

        // constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // setup worker request
        val workerRequest = OneTimeWorkRequestBuilder<PushProxWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        // enqueue
        workManagerInstance.beginUniqueWork(
            PROM_UNIQUE_WORK,
            ExistingWorkPolicy.KEEP,
            workerRequest,
        ).enqueue()
    }

    // if result is not null, it contains an error message
    fun turnPushProxOn() : String?{
        val error : String? = validatePushProxSettings()
        if(error != null){ return error }

        // idempotent call
        launchPushProxUsingWorkManager()

        _uiState.update { current ->
            current.copy(
                pushProxTurnedOn = true
            )
        }

        return null
    }

    fun turnPushProxOff(){
        val workerManagerInstance = WorkManager.getInstance(getContext())
        workerManagerInstance.cancelUniqueWork(PROM_UNIQUE_WORK)

        _uiState.update {current ->
            current.copy(
                pushProxTurnedOn = false
            )
        }
    }

    fun updatePushProxURL(url : String){
        _uiState.update { current ->
            current.copy(
                pushProxURL = url
            )
        }
    }

    fun updatePushProxFQDN(fqdn : String){
        _uiState.update { current ->
            current.copy(
                fqdn = fqdn
            )
        }
    }
}