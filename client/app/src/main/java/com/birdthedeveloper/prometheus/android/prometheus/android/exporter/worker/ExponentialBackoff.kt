package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.pow

class ExponentialBackoff {
    companion object {
        private const val multiplier: Double = 2.0
        private const val initialDelay = 3.0 // seconds

        suspend fun runWithBackoff(function: suspend () -> Unit, onException: () -> Unit) {
            var successfull: Boolean = false

            var currentDelay = initialDelay
            var currentExpIndex = -1

            while (!successfull) {
                try {
                    function()
                    successfull = true
                } catch (e: CancellationException) {
                    successfull = true
                } catch (e: Exception) {
                    // check for suppressed exceptions
                    for (exception in e.suppressed) {
                        if (exception is CancellationException) {
                            successfull = true
                        }
                    }

                    onException()

                    // calculate new delay
                    currentExpIndex++
                    currentDelay = initialDelay + multiplier.pow(currentExpIndex)
                }

                delay(currentDelay.toLong())
            }
        }
    }
}
