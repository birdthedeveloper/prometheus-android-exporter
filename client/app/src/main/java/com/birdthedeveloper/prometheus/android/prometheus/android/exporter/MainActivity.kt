package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.birdthedeveloper.prometheus.android.prometheus.android.exporter.ui.theme.PrometheusAndroidExporterTheme
import io.prometheus.client.Collector
//import io.prometheus.client.

class MainActivity : ComponentActivity() {

    // register custom prometheus exporter
    val requests: AndroidCustomExporter = AndroidCustomExporter().register()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    fun CollectMetrics(): String{
        //TODO convert this to string, so that it can be pushed through pushprox
        val collected: List<Collector.MetricFamilySamples> = requests.collect()
        collected.forEach { element ->
            element.help
        }
        //TextFormat()
        //TODO how to format this metric using not my function ?
        //TODO find this function in prometheus source code
        return ""
    }

    @Composable
    fun PrometheusHomepage() {
        Button(onClick = {
            println(CollectMetrics())
        }){
            Text("Click this button")
        }
    }
}

