package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

data class RemoteWriteConfiguration(
    val scrape_interval : Int,
    val remote_write_endpoint : String,
)

class RemoteWrite(config : RemoteWriteConfiguration) {

    //TODO implement this thing
}
