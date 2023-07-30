// Author: Martin Ptacek

package com.birdthedeveloper.prometheus.android.exporter.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.birdthedeveloper.prometheus.android.exporter.R
import com.birdthedeveloper.prometheus.android.exporter.compose.PromConfiguration
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exemplars.ExemplarConfig
import io.prometheus.client.exporter.common.TextFormat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import java.io.StringWriter

private const val TAG: String = "PROM_WORKER"

class PromWorker(
    private val context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    private val collectorRegistry = CollectorRegistry()
    private val metricsEngine: MetricsEngine = MetricsEngine(context)

    private lateinit var pushProxClient: PushProxClient
    private var remoteWriteSender: RemoteWriteSender? = null

    init {
        val androidCustomExporter = AndroidCustomExporter(metricsEngine)
        androidCustomExporter.register<AndroidCustomExporter>(collectorRegistry)
        ExemplarConfig.disableExemplars() // prometheus client library configuration
    }

    private fun performScrape(): String {
        val writer = StringWriter()
        TextFormat.write004(writer, collectorRegistry.metricFamilySamples())
        return writer.toString()
    }

    private fun countSuccessfulScrape() {
        Log.d(TAG, "Counting successful scrape")
        remoteWriteSender?.countSuccessfulScrape()
    }

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private suspend fun startServicesInOneThread(config: PromConfiguration) {
        val backgroundDispatcher = newFixedThreadPoolContext(1, "Prom worker")
        val threadContext = backgroundDispatcher.limitedParallelism(1)

        try {
            withContext(threadContext) {

                if (config.remoteWriteEnabled) {
                    remoteWriteSender = RemoteWriteSender(
                        RemoteWriteConfiguration(
                            scrapeInterval = config.remoteWriteScrapeInterval.toInt(),
                            remoteWriteEndpoint = config.remoteWriteEndpoint,
                            collectorRegistry = collectorRegistry,
                            exportInterval = config.remoteWriteExportInterval.toInt(),
                            maxSamplesPerExport = config.remoteWriteMaxSamplesPerExport.toInt(),
                            targetLabels = mapOf(
                                "job" to config.remoteWriteJobLabel,
                                "instance" to config.remoteWriteInstanceLabel,
                            ),
                        ) { context }
                    )
                    launch {
                        Log.d(TAG, "Remote Write launched")
                        remoteWriteSender?.start()
                    }
                }

                if (config.prometheusServerEnabled) {
                    launch {
                        Log.d(TAG, "Prometheus server launched")
                        PrometheusServer.start(
                            PrometheusServerConfig(
                                config.prometheusServerPort.toInt(),
                                ::performScrape,
                                ::countSuccessfulScrape,
                            ),
                        )
                    }
                }

                if (config.pushproxEnabled) {
                    pushProxClient = PushProxClient(
                        PushProxConfig(
                            pushProxUrl = config.pushproxProxyUrl,
                            performScrape = ::performScrape,
                            pushProxFqdn = config.pushproxFqdn,
                            registry = collectorRegistry,
                            countSuccessfulScrape = ::countSuccessfulScrape,
                            getContext = { context }
                        )
                    )
                    Log.d(TAG, "PushProx launching now")
                    launch {
                        Log.d(TAG, "PushProx launched")
                        pushProxClient.start()
                    }
                }
            }
        } finally {
            withContext(NonCancellable) {
                Log.v(TAG, "Canceling prom worker")
                backgroundDispatcher.close()
                metricsEngine.dispose()
            }
        }
    }

    override suspend fun doWork(): Result {
        val inputConfiguration: PromConfiguration = PromConfiguration.fromWorkData(inputData)
        Log.d(TAG, "Launching PromWorker with the following config: $inputConfiguration")

        setForeground(createForegroundInfo())

        startServicesInOneThread(inputConfiguration)

        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        // create a notification channel first
        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "CHANNEL_ID"
        val channel = NotificationChannel("CHANNEL_ID",
                "Android exporter channel", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "Prometheus Android Exporter"
        mNotificationManager.createNotificationChannel(channel)

        val title = "Prometheus android exporter running"

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setTicker(title)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .build()

        return ForegroundInfo(0, notification)
    }
}