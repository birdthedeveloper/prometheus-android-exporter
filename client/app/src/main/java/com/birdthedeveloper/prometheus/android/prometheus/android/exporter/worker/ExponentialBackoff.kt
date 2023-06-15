package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

private const val TAG : String = "EXPONENTIAL_BACKOFF"

class ExponentialBackoff {
    companion object {
        private const val multiplier: Double = 2.0
        private const val initialDelay = 3.0 // seconds
        private const val maxDelay = 40.0 // seconds

        suspend fun runWithBackoff(
            function: suspend () -> Unit,
            onException: () -> Unit, infinite: Boolean = true
        ) {

            var successfull = false

            var currentDelay: Double
            var currentExpIndex = -1

            while (!successfull) {
                try {
                    function()
                    successfull = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // check for suppressed exceptions
                    for (exception in e.suppressed) {
                        if (exception is CancellationException) {
                            throw exception
                        }
                    }

                    Log.d(TAG, "Exception caught")

                    onException()

                    // calculate new delay
                    currentExpIndex++
                    currentDelay = initialDelay + multiplier.pow(currentExpIndex)
                    currentDelay = min(currentDelay, maxDelay)

                    // finite vs infinite exponential backoff
                    if (currentDelay == maxDelay && !infinite) {
                        break
                    }

                    Log.d(TAG, "Backoff with delay: $currentDelay seconds")

                    delay(currentDelay.toLong() * 1000)
                }
            }
        }
    }
}
