package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.birdthedeveloper.prometheus.android.prometheus.android.exporter.R
import com.birdthedeveloper.prometheus.android.prometheus.android.exporter.compose.PromConfiguration
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import java.io.StringWriter

private const val TAG = "Worker"

class PromWorker(
    val context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    private val collectorRegistry = CollectorRegistry()
    private val metricsEngine: MetricsEngine = MetricsEngine(context)
    private lateinit var androidCustomExporter: AndroidCustomExporter
    private lateinit var pushProxClient: PushProxClient
    private var remoteWriteSender: RemoteWriteSender? = null

    //TODO foreground notification
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager

    private fun performScrape(): String {
        val writer = StringWriter()
        TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples())
        return writer.toString()
    }

    private fun initializeWork(config: PromConfiguration) {
        // initialize metrics
        androidCustomExporter = AndroidCustomExporter(metricsEngine).register(collectorRegistry)
    }

    private suspend fun countSuccessfulScrape(){
        remoteWriteSender?.countSuccessfulScrape()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun startServicesInOneThread(config: PromConfiguration){
        val threadContext = newSingleThreadContext("PromWorkerThreadContext")

        var deferred = coroutineScope {
            withContext(threadContext){

                if (config.remoteWriteEnabled) {
                    remoteWriteSender = RemoteWriteSender(
                        RemoteWriteConfiguration(
                            config.remoteWriteScrapeInterval,
                            config.remoteWriteEndpoint,
                            collectorRegistry,
                            {context},
                        )
                    )
                    launch {
                        remoteWriteSender?.start()
                    }
                }

                if (config.prometheusServerEnabled) {
                    launch {
                        PrometheusServer.start(
                            PrometheusServerConfig(
                                config.prometheusServerPort,
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
                        )
                    )
                    launch {
                        pushProxClient.start()
                    }
                }
            }
        }
    }

    override suspend fun doWork(): Result {
        val inputConfiguration: PromConfiguration = PromConfiguration.fromWorkData(inputData)

        // set foreground - //TODO is this right for the use case?
        //setForeground(createForegroundInfo())

        initializeWork(inputConfiguration)
        startServicesInOneThread(inputConfiguration)

        return Result.success()
    }

    //TODO foreground notification
    private fun createForegroundInfo(): ForegroundInfo {
        val id = "channel_id"
        val title = "title"
        val cancel = "cancel_download"
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(getId())

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText("progress")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return ForegroundInfo(1, notification)
    }

}