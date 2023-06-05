package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

private const val TAG = "SCRAPE_RECORDER"

//TODO implement this thing
// mutex with last scraped time
class ScrapeRecorder{

    //TODO mutex variable if mode is {pushprox / prometheus server} or {remote write}
    //TODO go back to mode {pushprox / prometheus server} only after N succesfull scrapes and no failures

    fun countSuccesfullScrape(){
        //TODO implement this thing
        // write to mutex that scrape has happend at this current time
        // set timer to 2 x remote_write_scrape_interval seconds to check if next scrape has happened
    }

    private fun onTimerTick(){
        //TODO implement this
        // check if other scrape has happened
        // if no scrape happened, go to mode {remote write}
        //

        //TODO finite state machine of this stuff !!!
    }
}