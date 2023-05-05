package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import kotlinx.coroutines.delay
import java.io.StringWriter

private val TAG = "PUSH_PROX_WORKER"

class PushProxWorker(
    private val context : Context,
    parameters : WorkerParameters
): CoroutineWorker(context, parameters){

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager

    override suspend fun doWork():Result {
        //TODO implement this
        val cache: PushProxWorkerCache = PushProxWorkerCache.getInstance {
            return@getInstance context
        }

        setForeground(createForegroundInfo())

        try{
            val pushProxConfig : PushProxConfig = PushProxConfig.fromData(inputData)

            cache.startBackground(pushProxConfig)

        }catch(e : Exception){
            Log.v(TAG, e.toString())
            return Result.failure()
        }

        return Result.success()
    }

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

        return ForegroundInfo(notificationId, notification)
    }
    
}

// thread-safe singleton
class PushProxWorkerCache private constructor(
    private val getContext: () -> Context
){
    private val collectorRegistry: CollectorRegistry = CollectorRegistry()
    private val metricsEngine : MetricsEngine = MetricsEngine(getContext())
    private val pushProxClient = PushProxClient(collectorRegistry, ::performScrape)
    private lateinit var androidCustomExporter : AndroidCustomExporter

    init {
        Log.v(TAG, "Initializing WorkerCache")
        androidCustomExporter = AndroidCustomExporter(metricsEngine).register(collectorRegistry)
    }

    private fun performScrape() : String{
        val writer = StringWriter()
        TextFormat.write004(writer, collectorRegistry.metricFamilySamples())
        return writer.toString()
    }

    suspend fun startBackground(pushProxConfig : PushProxConfig){
        pushProxClient.startBackground(pushProxConfig)
    }

    companion object {
        private var instance : PushProxWorkerCache? = null

        fun getInstance(getContext: () -> Context) : PushProxWorkerCache {
            if(instance == null){
                synchronized(PushProxWorkerCache::class.java){
                    if (instance == null){
                        instance = PushProxWorkerCache(getContext)
                    }
                }
            }
            return instance!!
        }
    }
}