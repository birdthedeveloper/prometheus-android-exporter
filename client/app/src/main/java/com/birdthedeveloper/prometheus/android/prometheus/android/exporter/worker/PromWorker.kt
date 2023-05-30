package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.content.Context
import android.util.Log
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import com.birdthedeveloper.prometheus.android.prometheus.android.exporter.compose.PromConfiguration

private val TAG = "Worker"

class PromWorker(
    val context : Context,
    val parameters : WorkerParameters,
) : RemoteCoroutineWorker(context = context, parameters = parameters) {

    override suspend fun doRemoteWork(): Result {
        val inputConfiguration : PromConfiguration = PromConfiguration.fromWorkData(inputData)

        while(true){
            Log.v(TAG, "Worker is working")
        }

        //TODO implement this asap

        return Result.success()
    }

}