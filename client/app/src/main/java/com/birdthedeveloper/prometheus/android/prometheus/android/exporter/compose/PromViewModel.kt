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
import com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker.PromWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.Exception

private val TAG : String = "PROMVIEWMODEL"

enum class ConfigFileState {
    LOADING, // parsing configuration file now
    ERROR, // file was not parsed successfully
    MISSING, // no configuration file has been found
    SUCCESS,
}

enum class UpdatePromConfig {
    PrometheusServerEnabled,
    PrometheusServerPort,
    PushproxEnabled,
    PushproxFqdn,
    PushproxProxyUrl,
    RemoteWriteEnabled,
    RemoteWriteScrapeInterval,
    RemoteWriteEndpoint,
}

enum class ExporterState{
    Running,
    NotRunning,
}

data class PromUiState(
    val tabIndex : Int = 0,
    val promConfig: PromConfiguration = PromConfiguration(),
    val configFileState : ConfigFileState = ConfigFileState.LOADING,
    val exporterState : ExporterState = ExporterState.NotRunning,
    val fileLoadException : String? = null
)


class PromViewModel(): ViewModel() {
    companion object {
        private const val PROM_UNIQUE_WORK : String = "prom_unique_job"
    }

    private val _uiState = MutableStateFlow(PromUiState())
    val uiState : StateFlow<PromUiState> = _uiState.asStateFlow()

    private lateinit var getContext: () -> Context

    private fun loadConfigurationFile(){
        Log.v(TAG, "Checking for configuration file")

        Log.v(TAG, getContext().filesDir.absolutePath)
        val fileExists = PromConfiguration.configFileExists(context = getContext())
        if (fileExists) {
            val tempPromConfiguration : PromConfiguration
            try {
                tempPromConfiguration = PromConfiguration.loadFromConfigFile(getContext())

                _uiState.update { current ->
                    current.copy(
                        promConfig = tempPromConfiguration,
                        configFileState = ConfigFileState.SUCCESS,
                    )
                }

            }catch (e : Exception){
                _uiState.update { current ->
                    current.copy(
                        configFileState = ConfigFileState.ERROR,
                        fileLoadException = e.toString(),
                    )
                }
            }
        }else {
            _uiState.update { current ->
                current.copy(configFileState = ConfigFileState.MISSING)
            }
        }
    }

    fun toggleIsRunning(){
        when(_uiState.value.exporterState) {
            ExporterState.Running -> {
                stopWorker()
            }
            ExporterState.NotRunning -> {
                startWorker()
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

    private fun startWorker(){
        Log.v(TAG, "Enqueuing work")
        val workManagerInstance = WorkManager.getInstance(getContext())

        // worker configuration
        val inputData : Data = _uiState.value.promConfig.toWorkData()

        // constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // setup worker request
        val workerRequest = OneTimeWorkRequestBuilder<PromWorker>()
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

    private fun stopWorker(){
        val workerManagerInstance = WorkManager.getInstance(getContext())
        workerManagerInstance.cancelUniqueWork(PROM_UNIQUE_WORK)
    }

    fun updatePromConfig(part : UpdatePromConfig, value : Any){
        when(part){
            UpdatePromConfig.PrometheusServerEnabled -> _uiState.update { current ->
                current.copy(promConfig = current.promConfig.copy(
                    prometheusServerEnabled = value as Boolean
                ))
            }
            UpdatePromConfig.PrometheusServerPort -> _uiState.update { current ->
                current.copy(promConfig = current.promConfig.copy(
                    prometheusServerPort = value as Int,
                ))
            }
            UpdatePromConfig.PushproxEnabled  -> _uiState.update { current ->
                current.copy(promConfig = current.promConfig.copy(
                    pushproxEnabled = value as Boolean,
                ))
            }
            UpdatePromConfig.PushproxFqdn -> _uiState.update { current ->
                current.copy(promConfig = current.promConfig.copy(
                    pushproxFqdn = value as String,
                ))
            }
            UpdatePromConfig.PushproxProxyUrl -> _uiState.update { current ->
                current.copy(promConfig = current.promConfig.copy(
                    pushproxProxyUrl = value as String,
                ))
            }
            UpdatePromConfig.RemoteWriteEnabled -> _uiState.update { current ->
                current.copy(promConfig = current.promConfig.copy(
                    remoteWriteEnabled = value as Boolean,
                ))
            }
            UpdatePromConfig.RemoteWriteScrapeInterval -> _uiState.update { current ->
                current.copy(promConfig = current.promConfig.copy(
                    remoteWriteScrapeInterval = value as Int,
                ))
            }
            UpdatePromConfig.RemoteWriteEndpoint -> _uiState.update { current ->
                current.copy(promConfig = current.promConfig.copy(
                    remoteWriteEndpoint = value as String,
                ))
            }
        }
    }
}
