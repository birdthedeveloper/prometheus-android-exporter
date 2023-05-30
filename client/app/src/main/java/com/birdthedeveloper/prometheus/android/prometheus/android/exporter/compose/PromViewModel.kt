package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.compose

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
import com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker.PushProxWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.Exception

enum class ConfigFileState {
    LOADING,
    ERROR, // file was not parsed succesfully
    MISSING,
    SUCCESS
}

enum class UpdatePromConfig {
    prometheusServerEnabled,
    prometheusServerPort,
    pushproxEnabled,
    pushproxFqdn,
    pushproxProxyUrl,
    remoteWriteEnabled,
    remoteWriteScrapeInterval,
    remoteWriteEndpoint,
}

data class PromUiState(
    val tabIndex : Int = 0,
    val promConfig: PromConfiguration = PromConfiguration(),
    val configFileState : ConfigFileState = ConfigFileState.LOADING,
)

private val TAG : String = "PROMVIEWMODEL"

class PromViewModel(): ViewModel() {
    private val PROM_UNIQUE_WORK : String = "prom_unique_job"


    private val _uiState = MutableStateFlow(PromUiState())
    val uiState : StateFlow<PromUiState> = _uiState.asStateFlow()

    private lateinit var getContext: () -> Context


    init {
        loadConfigurationFile()
    }

    private fun loadConfigurationFile(){
        Log.v(TAG, "Checking for configuration file")
        viewModelScope.launch {

            val fileExists = PromConfiguration.configFileExists(context = getContext())
            if (fileExists) {
                val tempPromConfiguration : PromConfiguration
                try {
                    tempPromConfiguration = PromConfiguration.loadFromConfigFile()

                    _uiState.update { current ->
                        current.copy(
                            promConfig = tempPromConfiguration,
                            configFileState = ConfigFileState.SUCCESS,
                        )
                    }

                }catch (e : Exception){
                    _uiState.update { current ->
                        current.copy(configFileState = ConfigFileState.ERROR)
                    }
                }
            }else{
                _uiState.update { current ->
                    current.copy(configFileState = ConfigFileState.MISSING)
                }
            }
        }
    }

    fun initializeWithApplicationContext(getContext : () -> Context){
        this.getContext = getContext
        loadConfigurationFile()
    }

    fun updateTabIndex(index : Int){
        _uiState.update {current ->
            current.copy(
                tabIndex =  index
            )
        }
    }

    fun startWorker(){
        val workManagerInstance = WorkManager.getInstance(getContext())

        // worker configuration
        val inputData : Data = _uiState.value.promConfig.toWorkData()

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

    fun stopWorker(){
        //TODO implement this thingy
        val workerManagerInstance = WorkManager.getInstance(getContext())
        workerManagerInstance.cancelUniqueWork(PROM_UNIQUE_WORK)
    }

    fun updatePromConfig(part : UpdatePromConfig, value : Any){
        when(part){
            UpdatePromConfig.prometheusServerEnabled -> _uiState.update { current ->
                current.copy(promConfig = current.promConfig.copy(
                    prometheusServerEnabled = value as Boolean
                ))
            }
            UpdatePromConfig.prometheusServerPort -> _uiState.update { current ->
                current.copy(promConfig = current.promConfig.copy(
                    prometheusServerPort = value as Int,
                ))
            }
            UpdatePromConfig.pushproxEnabled  -> _uiState.update { current ->
                current.copy(promConfig = current.promConfig.copy(
                    pushproxEnabled = value as Boolean,
                ))
            }
            UpdatePromConfig.pushproxFqdn -> _uiState.update { current ->
                current.copy(promConfig = current.promConfig.copy(
                    pushproxFqdn = value as String,
                ))
            }
            UpdatePromConfig.pushproxProxyUrl -> _uiState.update { current ->
                current.copy(promConfig = current.promConfig.copy(
                    pushproxProxyUrl = value as String,
                ))
            }
            UpdatePromConfig.remoteWriteEnabled -> _uiState.update { current ->
                current.copy(promConfig = current.promConfig.copy(
                    remoteWriteEnabled = value as Boolean,
                ))
            }
            UpdatePromConfig.remoteWriteScrapeInterval -> _uiState.update { current ->
                current.copy(promConfig = current.promConfig.copy(
                    remoteWriteScrapeInterval = value as Int,
                ))
            }
            UpdatePromConfig.remoteWriteEndpoint -> _uiState.update { current ->
                current.copy(promConfig = current.promConfig.copy(
                    remoteWriteEndpoint = value as String,
                ))
            }
        }
    }
}
