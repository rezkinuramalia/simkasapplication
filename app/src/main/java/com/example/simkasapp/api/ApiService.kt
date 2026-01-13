package com.example.simkasapp.api

import com.example.simkasapp.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*
import okhttp3.ResponseBody

interface ApiService {
    // AUTH
    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/auth/register")
    fun register(@Body request: RegisterRequest): Call<String>

    @GET("api/master/kelas")
    fun getKelasPublic(): Call<List<Kelas>>

    // PROFILE
    @GET("api/users/profile")
    fun getMyProfile(@Header("Authorization") token: String): Call<UserDto>

    @PUT("api/users/profile")
    fun updateProfile(@Header("Authorization") token: String, @Body req: UserProfileUpdateRequest): Call<UserDto>

    // MASTER DATA (Wadah/Kategori)
    // Arahkan ke endpoint MasterDataController yg sudah diperbaiki logic filternya
    @GET("api/master/kategori")
    fun getAllKategori(@Header("Authorization") token: String): Call<List<Kategori>>

    @POST("api/master/kategori")
    fun createKategori(@Header("Authorization") token: String, @Body req: KategoriRequest): Call<Kategori>

    // Master Kelas & Angkatan
    @GET("api/master/kelas")
    fun getAllKelas(@Header("Authorization") token: String): Call<List<Kelas>>

    @GET("api/master/angkatan")
    fun getAllAngkatan(@Header("Authorization") token: String): Call<List<Angkatan>>

    // DASHBOARD
    @GET("api/dashboard/kelas")
    fun getDashboardKelas(@Header("Authorization") token: String): Call<DashboardKelasResponse>

    @GET("api/dashboard/angkatan")
    fun getDashboardAngkatan(@Header("Authorization") token: String): Call<DashboardAngkatanResponse>

    // TRANSAKSI
    @Multipart
    @POST("api/transaksi")
    fun createTransaksi(
        @Header("Authorization") token: String,
        @Part("data") data: RequestBody,
        @Part file: MultipartBody.Part
    ): Call<TransaksiResponse>

    @GET("api/transaksi/history")
    fun getMyHistory(@Header("Authorization") token: String): Call<List<HistoryTransaksi>>


    @PUT("api/users/password")
    fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Call<ResponseBody> // Returns success/error message

    // ==========================================
    //  TAMBAHAN WAJIB UNTUK FITUR VALIDASI
    // ==========================================

    // 1. Endpoint untuk Validasi (Terima/Tolak)
    @PUT("api/transaksi/{id}/validasi")
    fun validasiTransaksi(
        @Header("Authorization") token: String,
        @Path("id") idTransaksi: Long,
        @Query("status") status: String // "VALID" atau "REJECTED"
    ): Call<TransaksiResponse>

    // 2. Endpoint List Pending per Wadah
    @GET("api/transaksi/pending/kategori/{id}")
    fun getPendingTransaksi(
        @Header("Authorization") token: String,
        @Path("id") idKategori: Long
    ): Call<List<TransaksiResponse>>

    // 3. Endpoint Total Pemasukan
    @GET("api/transaksi/total/kategori/{id}")
    fun getTotalPemasukan(
        @Header("Authorization") token: String,
        @Path("id") idKategori: Long
    ): Call<Double>

    @GET("api/master/kategori") // Pastikan ini memanggil endpoint yang menjalankan getKategoriForPaymentByUser()
    fun getKategori(): Call<List<Kategori>>
}