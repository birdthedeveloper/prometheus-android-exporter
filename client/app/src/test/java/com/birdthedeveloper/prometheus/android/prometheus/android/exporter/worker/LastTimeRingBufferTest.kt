package com.birdthedeveloper.prometheus.android.exporter.worker

import org.junit.Assert
import org.junit.Assert.*

import org.junit.Test

class LastTimeRingBufferTest {

    @Test
    fun `basic test of LastTimeRingBuffer`() {
        val lastTimeRingBuffer = LastTimeRingBuffer(10)
        assertEquals(0, lastTimeRingBuffer.getTimeByIndex(0))

        lastTimeRingBuffer.setLastTime(2L)
        lastTimeRingBuffer.setLastTime(5L)

        assertEquals(5, lastTimeRingBuffer.getTimeByIndex(0))
    }

    @Test
    fun `ring buffer test`() {
        val lastTimeRingBuffer = LastTimeRingBuffer(10)

        for (i in 1..4){
            lastTimeRingBuffer.setLastTime(i.toLong())
        }

        assertEquals(4, lastTimeRingBuffer.getTimeByIndex(0))
        assertEquals(3, lastTimeRingBuffer.getTimeByIndex(1))
        assertEquals(2, lastTimeRingBuffer.getTimeByIndex(2))
    }

    @Test
    fun `getTimeByIndex throws exception on out-of-bounds index`(){
        val lastTimeRingBuffer = LastTimeRingBuffer(10)
        assertThrows(IndexOutOfBoundsException::class.java){
            lastTimeRingBuffer.getTimeByIndex(Int.MAX_VALUE / 3)
        }
    }
}
