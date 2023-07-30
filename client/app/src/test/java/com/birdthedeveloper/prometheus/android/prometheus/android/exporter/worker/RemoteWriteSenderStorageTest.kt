// Author: Martin Ptacek

package com.birdthedeveloper.prometheus.android.exporter.worker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteWriteSenderStorageTest {
    @Test
    fun `basic test filterExpiredMetrics`() {
        val metrics: MutableList<MetricsScrape> = mutableListOf(
            // MetricSamples must be ordered
            createDummyMetricsScrape(70),
            createDummyMetricsScrape(50),
            createDummyMetricsScrape(30),
        )

        RemoteWriteSenderSimpleMemoryStorage.filterExpiredMetrics(metrics)

        assertEquals(2, metrics.size)

        // assert the right order
        val firstTimeStamp = metrics[0].timeSeriesList[0].sample.timeStampMs
        val secondTimeStamp = metrics[1].timeSeriesList[0].sample.timeStampMs
        assertTrue(firstTimeStamp < secondTimeStamp)
    }

    private fun createDummyMetricsScrape(ageInMinutes: Int): MetricsScrape {
        return MetricsScrape(
            timeSeriesList = listOf(
                StorageTimeSeries(
                    labels = listOf(),
                    sample = TimeSeriesSample(
                        // too old
                        timeStampMs = System.currentTimeMillis() - ageInMinutes * 60 * 1000L,
                        value = 0.0,
                    ),
                )
            )
        )
    }
}
