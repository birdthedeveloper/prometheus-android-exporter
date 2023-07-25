// Author: Martin Ptacek

package com.birdthedeveloper.prometheus.android.exporter.compose

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.birdthedeveloper.prometheus.android.exporter.worker.PromWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG: String = "PROMVIEWMODEL"

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
    RemoteWriteExportInterval,
    RemoteWriteMaxSamplesPerExport,
}

enum class ExporterState {
    Running,
    NotRunning,
    Enqueued,
}

data class PromUiState(
    val tabIndex: Int = 0,
    val promConfig: PromConfiguration = PromConfiguration(),
    val configFileState: ConfigFileState = ConfigFileState.LOADING,
    val exporterState: ExporterState = ExporterState.NotRunning,
    val fileLoadException: String? = null,
    val configValidationException: String? = null,
)

class PromViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PromUiState())
    val uiState: StateFlow<PromUiState> = _uiState.asStateFlow()

    private lateinit var getContext: () -> Context

    private var workerLiveData: LiveData<List<WorkInfo>>? = null
    private val workerLiveDataObserver: Observer<List<WorkInfo>> = Observer {
        Log.d(TAG, "Observing change in worker state")
        if (it.isEmpty()) {
            updateExporterStateWith(ExporterState.NotRunning)
        } else {
            when (it[0].state) {
                WorkInfo.State.ENQUEUED -> updateExporterStateWith(ExporterState.Enqueued)
                WorkInfo.State.RUNNING -> updateExporterStateWith(ExporterState.Running)
                WorkInfo.State.SUCCEEDED -> updateExporterStateWith(ExporterState.NotRunning)
                WorkInfo.State.FAILED -> updateExporterStateWith(ExporterState.NotRunning)
                WorkInfo.State.BLOCKED -> updateExporterStateWith(ExporterState.Enqueued)
                WorkInfo.State.CANCELLED -> updateExporterStateWith(ExporterState.NotRunning)
            }
        }
    }

    companion object {
        private const val PROM_UNIQUE_WORK: String = "prom_unique_job"
    }

    private fun loadConfigurationFile() {
        Log.d(TAG, "Checking for configuration file")

        val fileExists = PromConfiguration.configFileExists(context = getContext())
        if (fileExists) {
            val tempPromConfiguration: PromConfiguration
            try {
                tempPromConfiguration = PromConfiguration.loadFromConfigFile(getContext())

                _uiState.update { current ->
                    current.copy(
                        promConfig = tempPromConfiguration,
                        configFileState = ConfigFileState.SUCCESS,
                    )
                }

                Log.d(TAG, "Configuration file parsed successfully")

            } catch (e: Exception) {
                Log.d(TAG, "Configuration file parsing failed")
                _uiState.update { current ->
                    current.copy(
                        configFileState = ConfigFileState.ERROR,
                        fileLoadException = e.toString(),
                    )
                }
            }
        } else {
            _uiState.update { current ->
                current.copy(configFileState = ConfigFileState.MISSING)
            }
        }
    }

    fun toggleIsRunning() {
        when (_uiState.value.exporterState) {
            ExporterState.Running -> {
                stopWorker()
            }

            ExporterState.NotRunning -> {
                startWorker()
            }

            ExporterState.Enqueued -> {
                stopWorker()
            }
        }
    }

    fun dismissValidationExceptionDialog() {
        _uiState.update { current ->
            current.copy(
                configValidationException = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        workerLiveData?.removeObserver(workerLiveDataObserver)
    }

    private fun updateExporterStateWith(exporterState: ExporterState) {
        _uiState.update {
            it.copy(exporterState = exporterState)
        }
    }

    private fun startMonitoringWorker() {
        val workManagerInstance = WorkManager.getInstance(getContext())
        workerLiveData = workManagerInstance.getWorkInfosLiveData(
            WorkQuery.fromUniqueWorkNames(
                PROM_UNIQUE_WORK,
            ),
        )
        workerLiveData?.observeForever(workerLiveDataObserver)
    }


    fun initializeWithApplicationContext(getContext: () -> Context) {
        this.getContext = getContext
        loadConfigurationFile()
        startMonitoringWorker()
    }

    fun updateTabIndex(index: Int) {
        _uiState.update { current ->
            current.copy(
                tabIndex = index
            )
        }
    }

    private fun displayConfigValidationDialog(message: String): Boolean {
        Log.v(TAG, "Config Validation Message: $message")
        _uiState.update { current ->
            current.copy(
                configValidationException = message
            )
        }
        return false
    }

    private fun somePushProxVariableUnset(config: PromConfiguration): Boolean {
        return config.pushproxFqdn.isBlank() || config.pushproxProxyUrl.isBlank()
    }

    private fun somePrometheusServerVariableUnset(config: PromConfiguration): Boolean {
        return config.prometheusServerPort.isBlank()
    }

    private fun someRemoteWriteVariableUnset(config: PromConfiguration): Boolean {
        return config.remoteWriteEndpoint.isBlank()
                || config.remoteWriteScrapeInterval.isBlank()
                || config.remoteWriteExportInterval.isBlank()
                || config.remoteWriteMaxSamplesPerExport.isBlank()
    }

    private fun validatePromConfiguration(): Boolean {
        Log.d(TAG, "Validating PromConfiguration now")
        val config: PromConfiguration = uiState.value.promConfig

        // check either pushprox or prometheus server is turned on
        if (!config.pushproxEnabled && !config.prometheusServerEnabled) {
            return displayConfigValidationDialog("Please enable PushProx or Prometheus server!")
        }

        // check for empty configuration
        if (config.pushproxEnabled && somePushProxVariableUnset(config)) {
            return displayConfigValidationDialog("Please set all PushProx configuration settings!")
        }
        if (config.prometheusServerEnabled && somePrometheusServerVariableUnset(config)) {
            return displayConfigValidationDialog("Set all Prometheus Server config settings!")
        }
        if (config.remoteWriteEnabled && someRemoteWriteVariableUnset(config)) {
            return displayConfigValidationDialog("Set all Remote Write configuration settings!")
        }

        // validate settings for remote write
        if (config.remoteWriteEnabled) {
            // check scrape interval boundaries
            val minScrapeInterval = 1
            val maxScrapeInterval = 3600 / 4
            val scrapeInterval: Int = config.remoteWriteScrapeInterval.toIntOrNull()
                ?: return displayConfigValidationDialog("Scrape interval must be a number!")

            if (scrapeInterval > maxScrapeInterval || scrapeInterval < minScrapeInterval) {
                return displayConfigValidationDialog("Remote write scrape interval out of bounds!")
            }

            // check max samples per export
            config.remoteWriteMaxSamplesPerExport.toIntOrNull()
                ?: return displayConfigValidationDialog("Max Samples Per Export must be a number!")

            // check export interval
            val exportInterval: Int = config.remoteWriteExportInterval.toIntOrNull()
                ?: return displayConfigValidationDialog("Export interval must be a number!")
            if (scrapeInterval > exportInterval) {
                return displayConfigValidationDialog(
                    "Scrape interval must be smaller than Export interval!"
                )
            }
        }

        // validate settings for prometheus server
        if (config.prometheusServerEnabled) {
            // check port boundaries
            val minPort = 1024
            val maxPort = 65535
            Log.d(TAG, "Prometheus server port, ${config.prometheusServerPort}")
            val prometheusServerPort: Int = config.prometheusServerPort.toIntOrNull()
                ?: return displayConfigValidationDialog("Prometheus Server Port must be a number!")
            if (prometheusServerPort < minPort || prometheusServerPort > maxPort) {
                return displayConfigValidationDialog("Prometheus exporter port out of bounds!")
            }
        }

        // no need to validate anything for pushprox

        Log.d(TAG, "PromConfiguration validated")
        return true
    }

    private fun startWorker() {
        Log.d(TAG, "Starting worker")
        if (validatePromConfiguration()) {
            Log.v(TAG, "Enqueuing work")
            val workManagerInstance = WorkManager.getInstance(getContext())

            // worker configuration
            val inputData: Data = _uiState.value.promConfig.toWorkData()

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
    }

    private fun stopWorker() {
        Log.d(TAG, "Stopping worker")
        val workerManagerInstance = WorkManager.getInstance(getContext())
        workerManagerInstance.cancelUniqueWork(PROM_UNIQUE_WORK)
    }

    fun updatePromConfig(part: UpdatePromConfig, value: Any) {
        when (part) {
            UpdatePromConfig.PrometheusServerEnabled -> _uiState.update { current ->
                current.copy(
                    promConfig = current.promConfig.copy(
                        prometheusServerEnabled = value as Boolean
                    )
                )
            }

            UpdatePromConfig.PrometheusServerPort -> _uiState.update { current ->
                current.copy(
                    promConfig = current.promConfig.copy(
                        prometheusServerPort = value as String,
                    )
                )
            }

            UpdatePromConfig.PushproxEnabled -> _uiState.update { current ->
                current.copy(
                    promConfig = current.promConfig.copy(
                        pushproxEnabled = value as Boolean,
                    )
                )
            }

            UpdatePromConfig.PushproxFqdn -> _uiState.update { current ->
                current.copy(
                    promConfig = current.promConfig.copy(
                        pushproxFqdn = value as String,
                    )
                )
            }

            UpdatePromConfig.PushproxProxyUrl -> _uiState.update { current ->
                current.copy(
                    promConfig = current.promConfig.copy(
                        pushproxProxyUrl = value as String,
                    )
                )
            }

            UpdatePromConfig.RemoteWriteEnabled -> _uiState.update { current ->
                current.copy(
                    promConfig = current.promConfig.copy(
                        remoteWriteEnabled = value as Boolean,
                    )
                )
            }

            UpdatePromConfig.RemoteWriteScrapeInterval -> _uiState.update { current ->
                current.copy(
                    promConfig = current.promConfig.copy(
                        remoteWriteScrapeInterval = value as String,
                    )
                )
            }

            UpdatePromConfig.RemoteWriteEndpoint -> _uiState.update { current ->
                current.copy(
                    promConfig = current.promConfig.copy(
                        remoteWriteEndpoint = value as String,
                    )
                )
            }

            UpdatePromConfig.RemoteWriteExportInterval -> _uiState.update { current ->
                current.copy(
                    promConfig = current.promConfig.copy(
                        remoteWriteExportInterval = value as String
                    )
                )
            }

            UpdatePromConfig.RemoteWriteMaxSamplesPerExport -> _uiState.update { current ->
                current.copy(
                    promConfig = current.promConfig.copy(
                        remoteWriteMaxSamplesPerExport = value as String
                    )
                )
            }
        }
    }
}
