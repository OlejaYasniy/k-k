package com.example.indoornavigation.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

object RetrofitProvider {

    
    private const val USE_REAL_DEVICE = true


    private const val EMULATOR_URL     = "http://10.0.2.2:8080/"
    private const val REAL_DEVICE_URL  = "http://192.168.1.13:8080/"

    private val BASE_URL get() = if (USE_REAL_DEVICE) REAL_DEVICE_URL else EMULATOR_URL

    
    private val localeInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val appLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        val language = if (!appLocales.isEmpty) {
            appLocales.get(0)?.language ?: java.util.Locale.getDefault().language
        } else {
            java.util.Locale.getDefault().language
        }
        val newRequest = originalRequest.newBuilder()
            .addHeader("Accept-Language", language)
            .build()
        chain.proceed(newRequest)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(localeInterceptor)
        .connectTimeout(1500, java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(1500, java.util.concurrent.TimeUnit.MILLISECONDS)
        .writeTimeout(1500, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}