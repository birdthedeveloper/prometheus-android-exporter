package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.StringWriter

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
    val fqdn : String = "",
    val pushProxURL : String = "",
    val configFileState : ConfigFileState = ConfigFileState.LOADING,
)

private val TAG : String = "PROMVIEWMODEL"

class PromViewModel(): ViewModel() {
    private val DEFAULT_SERVER_PORT : Int = 10101 //TODO register with prometheus community
    private val _uiState = MutableStateFlow(PromUiState())
    val uiState : StateFlow<PromUiState> = _uiState.asStateFlow()

    // app - level components
    private val collectorRegistry: CollectorRegistry = CollectorRegistry()
    private lateinit var pushProxClient : PushProxClient
    private val prometheusServer : PrometheusServer = PrometheusServer()
    private lateinit var metricsEngine: MetricsEngine
    private lateinit var androidCustomExporter: AndroidCustomExporter

    init {
        Log.v(TAG, "initializing promviewmodel")
    }

    fun getDefaultPort() : Int {
        return DEFAULT_SERVER_PORT
    }

    fun setApplicationContext(getContext : () -> Context){
        // initalize app - level components
        metricsEngine = MetricsEngine(getContext())
        androidCustomExporter = AndroidCustomExporter(metricsEngine).register(collectorRegistry)
        pushProxClient = PushProxClient(collectorRegistry)
        //TODO somehow this PushProx definition does not work here
        // -> fix it
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

    private suspend fun performScrape() : String {
        val writer : StringWriter = StringWriter()
        TextFormat.write004(writer, collectorRegistry.metricFamilySamples())

        return writer.toString()
    }

    // if result is not null, it contains an error message
    fun turnServerOn() : String?{
        try{
            prometheusServer.startInBackground(
                PrometheusServerConfig(
                    getPromServerPort(),
                    ::performScrape
                )
            )
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
        val fqdn = _uiState.value.fqdn.trim()
        val url = _uiState.value.pushProxURL.trim()

        if( fqdn.isEmpty() ) return "Fully Qualified Domain Name cannot be empty!"
        if( url.isEmpty() ) return "PushProx URL cannot be empty!"

        return null
    }

    // if result is not null, it contains an error message
    fun turnPushProxOn() : String?{

        val error : String? = validatePushProxSettings()
        if(error != null){
            return error
        }

        try{
            pushProxClient.startBackground(
                PushProxConfig(
                    performScrape = ::performScrape,
                    fqdn = _uiState.value.fqdn.trim(),
                    proxyUrl = _uiState.value.pushProxURL.trim(),
                )
            )
        }catch(e : Exception){
            return "PushProx client failed!"
        }

        _uiState.update { current ->
            current.copy(
                pushProxTurnedOn = true
            )
        }

        return null
    }

    fun turnPushProxOff(){
        //TODO implement
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