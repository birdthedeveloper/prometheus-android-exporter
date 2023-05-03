package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import android.content.Context
import android.text.BoringLayout.Metrics
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
    val myFqdn : String? = null,
    val pushProxURL : String? = null,
)

val TAG : String = "PROMVIEWMODEL"
val DEFAULT_SERVER_PORT : Int = 10101 //TODO register with prometheus community

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

    // if result is not null, it contains an error message
    fun turnServerOn() : String?{
        //TODO implement

        return null;
    }

    fun turnServerOff(){
        //TODO implement
    }

    // if result is not null, it contains an error message
    fun turnPushProxOn() : String?{
        //TODO implement
        return null
    }

    fun turnPushProxOff(){
        //TODO implement
    }

    fun updatePushProxURL(url : String){
        //TODO implement
    }

    fun updatePushProxFQDN(fqdn : String){
        //TODO implement
    }
}