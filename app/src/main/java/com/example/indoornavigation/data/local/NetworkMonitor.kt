package com.example.indoornavigation.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.indoornavigation.data.remote.RetrofitProvider

class NetworkMonitor(private val context: Context) {

    val isConnected: Boolean
        get() {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isEmulator = RetrofitProvider.isEmulator
            return hasInternet || isEmulator || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }
}
