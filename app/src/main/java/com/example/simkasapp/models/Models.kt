package com.example.simkasapp.models

import java.io.Serializable // <--- 1. WAJIB ADA IMPORT INI

// ==========================================
// 1. AUTHENTICATION
// ==========================================
data class LoginRequest(val username: String, val password: String)

data class LoginResponse(
    val token: String,
    val username: String,
    val role: String?
)

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
// 2. MASTER DATA (Kategori, Kelas, Angkatan)
// ==========================================
data class Angkatan(val id: Int, val tahun: Int, val nama: String)
data class Kelas(val id: Int, val kode: String, val nama: String, val angkatanId: Int)

// 2. WAJIB TAMBAHKAN ': Serializable' DI BAWAH INI
data class Kategori(
    val id: Int,
    val nama: String,
    val keterangan: String?,
    val level: String?,
    val totalTerkumpul: Double
) : Serializable

data class KategoriRequest(val nama: String, val keterangan: String)

// ==========================================
// 3. PROFILE
// ==========================================
data class UserDto(
    val id: Int,
    val nim: String,
    val nama: String,
    val email: String,
    val phone: String?,
    val roleName: String,
    val kelasId: Int?,
    val angkatanId: Int?
)

data class UserProfileUpdateRequest(
    val nama: String,
    val email: String,
    val phone: String,
    val kelasId: Int?,
    val angkatanId: Int?
)

// ==========================================
// 4. DASHBOARD
// ==========================================
data class DashboardKelasResponse(
    val namaKelas: String,
    val totalPemasukan: Double,
    val totalPengeluaran: Double,
    val saldoKas: Double,
    val transaksiPending: Int,
    val anggotaBelumBayarBulanIni: Int
)

data class DashboardAngkatanResponse(
    val namaAngkatan: String,
    val totalPemasukan: Double,
    val totalPengeluaran: Double,
    val saldoKas: Double,
    val totalTransaksiPending: Int,
    val jumlahKelas: Int
)

// ==========================================
// 5. TRANSAKSI
// ==========================================
data class TransaksiResponse(
    val id: Long,
    val nominal: Double,
    val statusValidasi: String,
    val buktiBayar: String?,
    val jenisTransaksi: String?,
    val tanggalBayar: String?
)

data class TransaksiRequest(
    val idUser: Int,
    val nominal: Double,
    val bulanKas: Int,
    val tahunKas: Int,
    val keterangan: String,
    val jenisTransaksi: String,
    val idKategori: Int,
    val idKelas: Int?,
    val idAngkatan: Int?
)

data class HistoryTransaksi(
    val id: Long,
    val nominal: Double,
    val keterangan: String,
    val statusValidasi: String,
    val tanggalBayar: String?,
    val namaWadah: String?
)