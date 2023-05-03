package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PromUiState(
    val tabIndex : Int = 0,
    val serverTurnedOn : Boolean = false,
    val pushProxTurnedOn : Boolean = false,
    val serverPort : Int? = null, // if null, use default port
    val fqdn : String? = null,
    val pushProxURL : String? = null,
)

val TAG : String = "PROMVIEWMODEL"

class PromViewModel(): ViewModel() {
    private val _uiState = MutableStateFlow(PromUiState())
    val uiState : StateFlow<PromUiState> = _uiState.asStateFlow()

    // app - level components
    private val collectorRegistry: CollectorRegistry = CollectorRegistry()
    private val pushProxClient : PushProxClient = PushProxClient(collectorRegistry)
    private lateinit var metricsEngine: MetricsEngine
    private lateinit var customExporter: AndroidCustomExporter
    private lateinit var prometheusServer : PrometheusServer
    private lateinit var getApplicationContext : () -> Context

    init {
        Log.v(TAG, "initializing promviewmodel")
    }

    fun setApplicationContext(getContext : () -> Context){
        this.getApplicationContext = getContext

        // initalize app - level components
        this.metricsEngine = MetricsEngine(this.getApplicationContext())
        this.customExporter = AndroidCustomExporter(metricsEngine)
        this.prometheusServer = PrometheusServer()
    }

    fun updateTabIndex(index : Int){
        _uiState.update {current ->
            current.copy(
                tabIndex =  index
            )
        }
    }

    private fun getPromServerPort() : Int{
        val DEFAULT_SERVER_PORT : Int = 10101 //TODO register with prometheus community
        return if(_uiState.value.serverPort != null){
            _uiState.value.serverPort!!
        }else{
            DEFAULT_SERVER_PORT
        }
    }

    private suspend fun performScrape() : String {
        return ""
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
        _uiState.value.pushProxURL?: return "Please fill in PushProx URL with port"
        _uiState.value.fqdn?: return "Please fill in your Fully Qualified Domain Name"

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
                    fqdn = _uiState.value.fqdn!!,
                    proxyUrl = _uiState.value.pushProxURL!!,
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