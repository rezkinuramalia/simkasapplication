package com.example.simkasapp.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // IP LAPTOP KAMU: 192.168.1.18
    // Port Laragon/Springboot biasanya 8080
    private const val BASE_URL = "http://10.211.192.94:8080/api/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}