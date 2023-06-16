package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class Util {
    companion object{
        fun deviceIsConnectedToInternet(context : Context) : Boolean{
            val connectivityManager = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

            if (connectivityManager != null) {
                val network = connectivityManager.activeNetwork
                val cap = connectivityManager.getNetworkCapabilities(network)
                if (cap != null && cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    return true
                }
            }
            return false

        }
    }
}
