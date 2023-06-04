package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import remote.write.RemoteWrite.WriteRequest

data class RemoteWriteConfiguration(
    val scrape_interval : Int,
    val remote_write_endpoint : String,
)

class RemoteWriteSender(config : RemoteWriteConfiguration) {

    //TODO implement this thing

    fun test(){
        var request : WriteRequest
    }
}

