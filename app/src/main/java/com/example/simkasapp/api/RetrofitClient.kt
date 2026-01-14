package com.example.simkasapp.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // [PERBAIKAN FATAL]
    // HAPUS "/api/" DI BELAKANG URL!
    // ApiService sudah punya awalan "api/...", jadi di sini cukup sampai port 8080 saja.
    // Pastikan akhiri dengan tanda "/"
    const val BASE_URL = "http://192.168.1.10:8080/"

    private const val TAG = "RetrofitClient"

    // Logger untuk melihat request/response di Logcat (Penting buat debugging)
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // Waktu tunggu connect
        .readTimeout(30, TimeUnit.SECONDS)    // Waktu tunggu respon
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)   // Pasang Logger
        .addInterceptor { chain ->
            val original = chain.request()

            // Log URL yang sedang ditembak (Biar ketahuan kalau salah alamat)
            Log.d(TAG, "Hitting: ${original.url}")

            // Kita biarkan request apa adanya.
            // Token "Bearer" sudah ditangani manual di ApiService atau logic LoginScreen
            // Jadi interceptor ini cukup meloloskan request saja biar gak ribet/konflik.
            chain.proceed(original)
        }
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}