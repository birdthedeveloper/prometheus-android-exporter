package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

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

    override suspend fun doWork():Result {
        //TODO implement this
        val cache: PushProxWorkerCache = PushProxWorkerCache.getInstance {
            return@getInstance context
        }

        try{
            val pushProxConfig : PushProxConfig = PushProxConfig.fromData(inputData)

            cache.startBackground(pushProxConfig)

        }catch(e : Exception){
            Log.v(TAG, e.toString())
            return Result.failure()
        }

        return Result.success()
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