package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import org.junit.Assert
import org.junit.Assert.*

import org.junit.Test

class LastTimeRingBufferTest {

    @Test
    fun `basic test of LastTimeRingBuffer`() {
        val lastTimeBuffer = LastTimeRingBuffer(10)
        assertEquals(0, lastTimeBuffer.getTimeByIndex(0))

        lastTimeBuffer.setLastTime(2L)
        lastTimeBuffer.setLastTime(5L)

        //assertEquals(5, lastTimeBuffer.getTimeByIndex(0))
        //assertEquals(2, lastTimeBuffer.getTimeByIndex(1))
    }
}