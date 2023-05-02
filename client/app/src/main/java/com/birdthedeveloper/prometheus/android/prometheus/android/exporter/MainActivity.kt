package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import android.os.Bundle
import android.text.BoringLayout.Metrics
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.birdthedeveloper.prometheus.android.prometheus.android.exporter.ui.theme.PrometheusAndroidExporterTheme
import io.prometheus.client.Collector
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.StringWriter

//https://www.geeksforgeeks.org/how-to-launch-an-application-automatically-on-system-boot-up-in-android/

// reference android app architecture
//https://github.dev/google-developer-training/basic-android-kotlin-compose-training-unscramble


// reference for sharing the view model accross whole application
// just acces viewmodel instance using viewmodel() function in composable fun
class MainActivity : ComponentActivity() {

    private val collectorRegistry: CollectorRegistry = CollectorRegistry()
    private lateinit var metricsEngine: MetricsEngine
    private lateinit var customExporter: AndroidCustomExporter

    private var pushProxStarted : Boolean = false
    private lateinit var pushProxClient : PushProxClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize()

        setContent {
            PrometheusAndroidExporterTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    PrometheusHomepage()
                }
            }
        }
    }

    private fun initialize (){
        metricsEngine = MetricsEngine(this.applicationContext)
        customExporter = AndroidCustomExporter(metricsEngine).register(collectorRegistry)
    }

    private suspend fun reallyCollectMetrics() : String {
        delay(500)
        val writer = StringWriter()
        TextFormat.write004(writer, collectorRegistry.metricFamilySamples())
        return writer.toString()
    }

    private fun CollectMetrics(): String{
        val writer = StringWriter()
        TextFormat.write004(writer, collectorRegistry.metricFamilySamples())

        // initialize PushProx
        if (!pushProxStarted) {
            pushProxClient = PushProxClient(
                config = PushProxConfig(
                    "test.example.com",
                    "http://143.42.59.63:8080",
                    1,
                    5,
                    collectorRegistry,
                    ::reallyCollectMetrics,
                )
            )
            pushProxClient.startBackground()
            pushProxStarted = true
        }

        return writer.toString()
    }

    private fun startPromServer(){
        //TODO impl
        val promServer : PrometheusServer = PrometheusServer(
            config = PrometheusServerConfig(
                8080,
                ::reallyCollectMetrics
            )
        )
        promServer.startBackground()
    }

    @Composable
    fun PrometheusHomepage() {
        Column {
            Button(onClick = {
                println(CollectMetrics())
            }) {
                Text("Click this button")
            }
            Button(onClick = ::startPromServer){
                Text("Prometheus server")
            }
        }
    }
}

//TODO how to call coroutine / async from custom collector
//TODO how to extract any hw system metric from android API
//TODO how to get permission on first application start only
//TODO viewmodel, state management

//class GlobalViewModel : ViewModel() {
//    val state = mutableStateOf<Int>(Resource.Success(i))
//
//    fun deleteItem(id: Int) {
//        viewModelScope.launch {
//            deleteItemInternal(id).collect { response ->
//                state.value = response
//            }
//        }
//    }
//
//    suspend fun deleteItemInternal(id: Int) = flow {
//        try {
//            emit(Resource.Loading)
//            delay(1000)
//            if (id % 3 == 0) {
//                throw IllegalStateException("error on third")
//            }
//            emit(Resource.Success(id))
//        } catch (e: Exception) {
//            emit(Resource.Failure(e.message ?: e.toString()))
//        }
//    }
//}
//
