package com.example.simkasapp.api

import com.example.simkasapp.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*
import okhttp3.ResponseBody

interface ApiService {
    // --- AUTH & PROFILE (TETAP) ---
    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/auth/register")
    fun register(@Body request: RegisterRequest): Call<String>

    @GET("api/users/profile")
    fun getMyProfile(@Header("Authorization") token: String): Call<UserDto>

    @PUT("api/users/profile")
    fun updateProfile(@Header("Authorization") token: String, @Body req: UserProfileUpdateRequest): Call<UserDto>

    @PUT("api/users/password")
    fun changePassword(@Header("Authorization") token: String, @Body request: ChangePasswordRequest): Call<ResponseBody>

    // --- MASTER DATA KELAS & ANGKATAN (TETAP) ---
    @GET("api/master/kelas")
    fun getAllKelas(@Header("Authorization") token: String): Call<List<Kelas>>

    @GET("api/master/kelas")
    fun getKelasPublic(): Call<List<Kelas>>

    @GET("api/master/angkatan")
    fun getAllAngkatan(@Header("Authorization") token: String): Call<List<Angkatan>>

    // --- DASHBOARD (TETAP) ---
    @GET("api/dashboard/kelas")
    fun getDashboardKelas(@Header("Authorization") token: String): Call<DashboardKelasResponse>

    @GET("api/dashboard/angkatan")
    fun getDashboardAngkatan(@Header("Authorization") token: String): Call<DashboardAngkatanResponse>

    // ==========================================
    // [PERBAIKAN UTAMA] KATEGORI / WADAH
    // ==========================================

    // 1. UNTUK MENU BERANDA (KELOLA): Hanya wadah buatan sendiri
    @GET("api/master/kategori/managed")
    fun getKategoriManaged(@Header("Authorization") token: String): Call<List<Kategori>>

    // 2. UNTUK MENU BAYAR (SETOR): Hanya wadah tagihan
    @GET("api/master/kategori/payment")
    fun getKategoriPayment(@Header("Authorization") token: String): Call<List<Kategori>>

    // 3. UNTUK TOMBOL NONAKTIFKAN
    @PUT("api/master/kategori/{id}/status")
    fun updateStatusKategori(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Query("aktif") aktif: Boolean
    ): Call<Kategori>

    // 4. CREATE KATEGORI
    @POST("api/master/kategori")
    fun createKategori(@Header("Authorization") token: String, @Body req: KategoriRequest): Call<Kategori>

    // 5. GET GENERAL (Backup)
    @GET("api/master/kategori")
    fun getAllKategori(@Header("Authorization") token: String): Call<List<Kategori>>

    // --- TRANSAKSI (TETAP) ---
    @Multipart
    @POST("api/transaksi")
    fun createTransaksi(
        @Header("Authorization") token: String,
        @Part("data") data: RequestBody,
        @Part file: MultipartBody.Part
    ): Call<TransaksiResponse>

    @GET("api/transaksi/history")
    fun getMyHistory(@Header("Authorization") token: String): Call<List<HistoryTransaksi>>

    @PUT("api/transaksi/{id}/validasi")
    fun validasiTransaksi(
        @Header("Authorization") token: String,
        @Path("id") idTransaksi: Long,
        @Query("status") status: String,
        @Query("catatan") catatan: String?
    ): Call<TransaksiResponse>

    @GET("api/transaksi/pending/kategori/{id}")
    fun getPendingTransaksi(@Header("Authorization") token: String, @Path("id") idKategori: Long): Call<List<TransaksiResponse>>

    @GET("api/transaksi/total/kategori/{id}")
    fun getTotalPemasukan(@Header("Authorization") token: String, @Path("id") idKategori: Long): Call<Double>
}