package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import androidx.work.Data

data class PushProxConfig(
    val pushProxUrl : String,
    val pushProxFqdn : String,
){
    companion object{
        fun fromData(data : Data) : PushProxConfig{
            return PushProxConfig(
                data.getString("0")!!,
                data.getString("1")!!,
            )
        }
    }

    fun toData() : Data {
        return Data.Builder()
            .putString("0", pushProxUrl)
            .putString("1", pushProxFqdn)
            .build()
    }
}

data class PromServerConfig(
    //TODO implement this
    val dummy : String,
)
