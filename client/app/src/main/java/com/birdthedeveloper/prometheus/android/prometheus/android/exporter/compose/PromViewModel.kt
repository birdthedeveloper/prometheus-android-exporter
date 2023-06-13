package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.compose

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
import com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker.PromWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private val TAG: String = "PROMVIEWMODEL"

enum class ConfigFileState {
    LOADING, // parsing configuration file now
    ERROR, // file was not parsed successfully
    MISSING, // no configuration file has been found
    SUCCESS,
}

class PromUiConfiguration private constructor(
    val prometheusServerEnabled: Boolean,
    val prometheusServerPort: String,
    val pushproxEnabled: Boolean,
    val pushproxFqdn: String,
    val pushproxProxyUrl: String,
    val remoteWriteEnabled: Boolean,
    val remoteWriteScrapeInterval: String,
    val remoteWriteEndpoint: String,
    val remoteWriteExportInterval : String,
    val remoteWriteMaxSamplesPerExport : String,
){
    companion object {
        fun default() : PromUiConfiguration{
            val template = PromConfiguration()

            return PromUiConfiguration(
                remoteWriteEndpoint = template.remoteWriteEndpoint,
                prometheusServerPort = template.prometheusServerPort.toString(),
                prometheusServerEnabled = //TODO asap
            )
        }
    }

    // Throws exception when values are illegal
    fun toPromConfiguration() : PromConfiguration {
        //TODO
    }
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
    RemoteWriteexportInterval,
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

class PromViewModel() : ViewModel() {

    private val _uiState = MutableStateFlow(PromUiState())
    val uiState: StateFlow<PromUiState> = _uiState.asStateFlow()

    private lateinit var getContext: () -> Context

    private var workerLiveData: LiveData<List<WorkInfo>>? = null
    private val workerLiveDataObserver : Observer<List<WorkInfo>> = Observer {
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
        Log.v(TAG, "Checking for configuration file")

        Log.v(TAG, getContext().filesDir.absolutePath)
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

            } catch (e: Exception) {
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

    override fun onCleared(){
        super.onCleared()
        workerLiveData?.removeObserver(workerLiveDataObserver)
    }

    private fun updateExporterStateWith(exporterState: ExporterState){
        _uiState.update {
            it.copy(exporterState = exporterState)
        }
    }

    private fun startMonitoringWorker(){
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

    private fun validatePromConfiguration(): Boolean {
        val config: PromConfiguration = uiState.value.promConfig

        // check eather pushprox or prometheus server is on
        if (!config.pushproxEnabled && !config.prometheusServerEnabled) {
            return displayConfigValidationDialog("Please enable PushProx or Prometheus server!")
        }

        // check port boundaries
        val minPort = 1024
        val maxPort = 65535
        if (config.prometheusServerPort < minPort || config.prometheusServerPort > maxPort) {
            return displayConfigValidationDialog("Prometheus exporter port out of bounds!")
        }

        // check scrape interval boundaries
        val minScrapeInterval = 1
        val maxScrapeInterval = 3600 / 4
        val scrapeInterval = config.remoteWriteScrapeInterval
        if (scrapeInterval > maxScrapeInterval || scrapeInterval < minScrapeInterval) {
            return displayConfigValidationDialog("Remote write scrape interval out of bounds!")
        }

        // if remote write enabled, remote_write_endpoint is set
        if (config.remoteWriteEnabled && config.remoteWriteEndpoint.isBlank()) {
            return displayConfigValidationDialog("Please set remote write endpoint!")
        }

        // if pushprox is enabled, fqdn is set
        if (config.pushproxEnabled && config.pushproxFqdn.isBlank()) {
            return displayConfigValidationDialog(
                "Please set proxy fqdn! For example: test.example.com"
            )
        }

        // if pushprox is enabled, proxy_url is set
        if (config.pushproxEnabled && config.pushproxProxyUrl.isBlank()) {
            return displayConfigValidationDialog("Please set proxy_url!")
        }

        return true
    }

    private fun startWorker() {
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

            // set UI state
            _uiState.update { current ->
                current.copy(exporterState = ExporterState.Running)
            }
        }
    }

    private fun stopWorker() {
        val workerManagerInstance = WorkManager.getInstance(getContext())
        workerManagerInstance.cancelUniqueWork(PROM_UNIQUE_WORK)

        // update UI state
        _uiState.update { current ->
            current.copy(exporterState = ExporterState.NotRunning)
        }
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
                        prometheusServerPort = value as Int,
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
                        remoteWriteScrapeInterval = value as Int,
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

            UpdatePromConfig.RemoteWriteexportInterval -> _uiState.update {current ->
                current.copy(
                    promConfig = current.promConfig.copy(
                        remoteWriteExportInterval = value as Int
                    )
                )
            }

            UpdatePromConfig.RemoteWriteMaxSamplesPerExport -> _uiState.update { current ->
                current.copy(
                    promConfig = current.promConfig.copy(
                        remoteWriteMaxSamplesPerExport = value as Int
                    )
                )
            }
        }
    }
}
