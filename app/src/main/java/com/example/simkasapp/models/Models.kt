package com.example.simkasapp.models

import java.io.Serializable

// ==========================================
// 1. AUTHENTICATION
// ==========================================
data class LoginRequest(val username: String, val password: String)

data class LoginResponse(
    val token: String,
    val username: String,
    val id: Int,
    val role: String?
) : Serializable

data class RegisterRequest(
    val nama: String,
    val nim: String,
    val email: String,
    val password: String,
    val roleId: Int = 3,
    val kelasId: Int? = null,
    val angkatanId: Int? = null
)

// ==========================================
// 2. MASTER DATA
// ==========================================
data class Kelas(
    val id: Int,
    val kode: String,
    val nama: String,
    val angkatanId: Int?,
    val namaAngkatan: String?
) : Serializable

data class Angkatan(val id: Int, val tahun: Int, val nama: String) : Serializable

data class Kategori(
    val id: Long,
    val nama: String,
    val keterangan: String?,
    val level: String?,
    val nominal: Double = 0.0,
    val totalTerkumpul: Double = 0.0,
    val aktif: Boolean = true,
) : Serializable

data class KategoriRequest(val nama: String, val keterangan: String, val nominal: Double = 0.0)

// ==========================================
// 3. PROFILE
// ==========================================
data class UserDto(
    val id: Int, val nim: String, val nama: String, val email: String,
    val phone: String?, val roleName: String, val kelasId: Int?, val angkatanId: Int?
) : Serializable

data class UserProfileUpdateRequest(
    val nama: String, val email: String, val phone: String,
    val kelasId: Int?, val angkatanId: Int?
)

// ==========================================
// 4. DASHBOARD
// ==========================================
data class DashboardKelasResponse(
    val namaKelas: String, val totalPemasukan: Double, val totalPengeluaran: Double,
    val saldoKas: Double, val transaksiPending: Int, val anggotaBelumBayarBulanIni: Int
) : Serializable

data class DashboardAngkatanResponse(
    val namaAngkatan: String, val totalPemasukan: Double, val totalPengeluaran: Double,
    val saldoKas: Double, val totalTransaksiPending: Int, val jumlahKelas: Int
) : Serializable

// ==========================================
// 5. TRANSAKSI
// ==========================================
data class TransaksiResponse(
    val id: Long,
    val idUser: Long?, // [PERBAIKAN] Ganti Int ke Long? agar aman
    val nominal: Double,
    val tanggalBayar: String?, // Backend sekarang mengirim String, jadi ini Aman
    val keterangan: String?,
    val jenisTransaksi: String?,
    val statusValidasi: String?,
    val buktiBayar: String?,
    val catatanAdmin: String? = null,

    // Field Tambahan
    val namaPengirim: String?,
    val nimPengirim: String?,
    val namaKelas: String?,
    val namaWadah: String?
) : Serializable


data class TransaksiRequest(
    val idUser: Int, val idKategori: Int, val nominal: Double,
    val bulanKas: Int, val tahunKas: Int, val keterangan: String,
    val jenisTransaksi: String, val idKelas: Int?, val idAngkatan: Int?,
    val buktiBayar: String? = null
)

data class HistoryTransaksi(
    val id: Long, val nominal: Double, val keterangan: String,
    val statusValidasi: String, val tanggalBayar: String?, val namaWadah: String?, val catatanAdmin: String? = null
) : Serializable

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)