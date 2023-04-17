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
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import java.io.StringWriter

class MainActivity : ComponentActivity() {

    // register custom prometheus exporter
    val collectorRegistry: CollectorRegistry = CollectorRegistry()
    val customExporter: AndroidCustomExporter = AndroidCustomExporter()
        .register(collectorRegistry)

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
        val writer = StringWriter()
        TextFormat.write004(writer, collectorRegistry.metricFamilySamples())
        return writer.toString()
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

