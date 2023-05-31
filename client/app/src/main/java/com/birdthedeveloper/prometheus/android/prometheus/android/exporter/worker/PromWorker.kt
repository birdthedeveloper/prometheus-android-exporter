package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.birdthedeveloper.prometheus.android.prometheus.android.exporter.compose.PromConfiguration
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import io.ktor.client.request.*

private val TAG = "Worker"

class PromWorker(
    context : Context,
    parameters : WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val inputConfiguration : PromConfiguration = PromConfiguration.fromWorkData(inputData)

        while(true){
            Log.v(TAG, "Worker is working " + LocalDateTime.now().toString())
            //TODO curl localhost

            delay(1000L)
        }

        //TODO implement this asap

        return Result.success()
    }

}