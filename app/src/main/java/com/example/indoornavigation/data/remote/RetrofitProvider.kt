package com.example.indoornavigation.data.remote

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitProvider {

    private const val EMULATOR_URL    = "http://10.0.2.2:8080/"
    // Your computer's current IP address on Wi-Fi is 10.99.92.243, and on Radmin VPN is 26.76.124.81
    private const val REAL_DEVICE_URL = "http://192.168.1.13:8080/"

    val isEmulator: Boolean
        get() = (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MODEL.startsWith("sdk_")
                || android.os.Build.BOARD.contains("goldfish")
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu")
                || android.os.Build.PRODUCT.contains("sdk_")
                || android.os.Build.PRODUCT.contains("google_sdk")
                || android.os.Build.PRODUCT.contains("emulator")
                || android.os.Build.PRODUCT.contains("simulator")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk" == android.os.Build.PRODUCT)

    private val BASE_URL get() = if (isEmulator) EMULATOR_URL else REAL_DEVICE_URL

    // Reads language directly from SharedPreferences — always up-to-date, no restart needed
    private fun currentLang(context: Context): String {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return prefs.getString("language", "ru") ?: "ru"
    }

    private fun makeClient(context: Context): OkHttpClient {
        val localeInterceptor = Interceptor { chain ->
            val lang = currentLang(context)
            chain.proceed(
                chain.request().newBuilder()
                    .header("Accept-Language", lang)
                    .build()
            )
        }
        return OkHttpClient.Builder()
            .addInterceptor(localeInterceptor)
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    // Stateless: each call builds API with current context/language
    fun create(context: Context): ApiService =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(makeClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

    // Legacy singleton (used where context not available) — uses system locale
    val api: ApiService by lazy {
        val localeInterceptor = Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("Accept-Language", java.util.Locale.getDefault().language)
                    .build()
            )
        }
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(localeInterceptor)
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}