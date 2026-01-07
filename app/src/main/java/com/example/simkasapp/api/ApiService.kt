package com.example.simkasapp.api

import com.example.simkasapp.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // ==========================================
    // 1. AUTHENTICATION
    // ==========================================
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<String>

    // ==========================================
    // 2. PROFILE USER
    // ==========================================
    @GET("users/profile")
    fun getMyProfile(@Header("Authorization") token: String): Call<UserDto>

    @PUT("users/profile")
    fun updateProfile(@Header("Authorization") token: String, @Body req: UserProfileUpdateRequest): Call<UserDto>

    // ==========================================
    // 3. MASTER DATA
    // ==========================================
    // --- Kategori / Wadah ---
    @GET("/api/master/kategori")
    fun getAllKategori(@Header("Authorization") token: String): Call<List<Kategori>>

    @POST("master/kategori")
    fun createKategori(@Header("Authorization") token: String, @Body req: KategoriRequest): Call<Kategori>

    // --- Kelas & Angkatan (INI YANG BARU DITAMBAHKAN) ---
    // --- MASTER DATA ---
    // Tambahkan "api/" di depannya agar sesuai dengan controller yang baru diperbaiki

    @GET("api/master/kelas")
    fun getAllKelas(@Header("Authorization") token: String): Call<List<Kelas>>

    @GET("api/master/angkatan")
    fun getAllAngkatan(@Header("Authorization") token: String): Call<List<Angkatan>>

    // ==========================================
    // 4. DASHBOARD
    // ==========================================
    @GET("dashboard/kelas")
    fun getDashboardKelas(@Header("Authorization") token: String): Call<DashboardKelasResponse>

    @GET("dashboard/angkatan")
    fun getDashboardAngkatan(@Header("Authorization") token: String): Call<DashboardAngkatanResponse>

    // ==========================================
    // 5. TRANSAKSI (UPLOAD & HISTORY)
    // ==========================================
    @Multipart
    @POST("transaksi")
    fun createTransaksi(
        @Header("Authorization") token: String,
        @Part("data") data: RequestBody,      // Data JSON
        @Part file: MultipartBody.Part        // File Gambar
    ): Call<TransaksiResponse>

    @GET("transaksi/history")
    fun getMyHistory(@Header("Authorization") token: String): Call<List<HistoryTransaksi>>
}