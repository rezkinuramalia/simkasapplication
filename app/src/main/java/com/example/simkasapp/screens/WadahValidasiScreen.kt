package com.example.simkasapp.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.simkasapp.api.RetrofitClient
import com.example.simkasapp.models.Kategori
import com.example.simkasapp.models.TransaksiResponse
import com.example.simkasapp.ui.theme.BpsBlue
import com.example.simkasapp.ui.theme.BpsGreen
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WadahValidasiScreen(navController: NavController, token: String, wadah: Kategori) {
    // Variable state untuk menampung data list dan total
    var listData by remember { mutableStateOf<List<TransaksiResponse>>(emptyList()) }
    var totalMasuk by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Fungsi untuk mengambil data dari Backend
    fun loadData() {
        isLoading = true
        val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        // 1. Ambil List Transaksi (Backend sudah mengurutkan: Pending di atas, Valid/Rejected di bawah)
        RetrofitClient.instance.getPendingTransaksi(authToken, wadah.id).enqueue(object : Callback<List<TransaksiResponse>> {
            override fun onResponse(call: Call<List<TransaksiResponse>>, response: Response<List<TransaksiResponse>>) {
                if (response.isSuccessful) {
                    listData = response.body() ?: emptyList()
                } else {
                    Toast.makeText(context, "Gagal memuat list: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
                isLoading = false
            }
            override fun onFailure(call: Call<List<TransaksiResponse>>, t: Throwable) {
                isLoading = false
                Toast.makeText(context, "Error koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
                t.printStackTrace()
            }
        })

        // 2. Ambil Total Pemasukan Valid
        RetrofitClient.instance.getTotalPemasukan(authToken, wadah.id).enqueue(object : Callback<Double> {
            override fun onResponse(call: Call<Double>, response: Response<Double>) {
                if (response.isSuccessful) {
                    totalMasuk = response.body() ?: 0.0
                }
            }
            override fun onFailure(call: Call<Double>, t: Throwable) {}
        })
    }

    // Panggil loadData saat halaman dibuka
    LaunchedEffect(Unit) { loadData() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // --- HEADER ---
        Text(wadah.nama, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = BpsBlue)
        Text("Total Pemasukan: ${formatRupiahLocal(totalMasuk)}", color = BpsGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)

        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Daftar Pengajuan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Menunggu konfirmasi di paling atas", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        // --- CONTENT LIST ---
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (listData.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Belum ada data transaksi.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listData) { trans ->
                    // Memanggil Card Custom di bawah
                    TransactionCardItem(
                        trans = trans,
                        onAccept = {
                            doValidasi(context, token, trans.id, "VALID") { loadData() }
                        },
                        onReject = {
                            doValidasi(context, token, trans.id, "REJECTED") { loadData() }
                        }
                    )
                }
            }
        }
    }
}

// Fungsi Helper untuk melakukan Validasi ke API
fun doValidasi(context: android.content.Context, token: String, id: Long, status: String, onSuccess: () -> Unit) {
    val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

    RetrofitClient.instance.validasiTransaksi(authToken, id, status, null).enqueue(object : Callback<TransaksiResponse> {
        override fun onResponse(call: Call<TransaksiResponse>, response: Response<TransaksiResponse>) {
            if (response.isSuccessful) {
                Toast.makeText(context, "Status diubah menjadi $status", Toast.LENGTH_SHORT).show()
                onSuccess() // Refresh halaman
            } else {
                Toast.makeText(context, "Gagal mengubah status", Toast.LENGTH_SHORT).show()
            }
        }
        override fun onFailure(call: Call<TransaksiResponse>, t: Throwable) {
            Toast.makeText(context, "Error koneksi", Toast.LENGTH_SHORT).show()
        }
    })
}

// --- KOMPONEN CARD TRANSAKSI (CUSTOM) ---
@Composable
fun TransactionCardItem(trans: TransaksiResponse, onAccept: () -> Unit, onReject: () -> Unit) {
    // Cek apakah statusnya masih PENDING
    val isPending = (trans.statusValidasi == "PENDING")

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // BAGIAN KIRI: Info Transaksi
            Column(modifier = Modifier.weight(1f)) {
                // Tampilkan Nama Pengirim (jika ada, kalau null tampilkan "Mahasiswa")
                Text(
                    text = trans.namaPengirim ?: "Mahasiswa",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = formatRupiahLocal(trans.nominal),
                    fontWeight = FontWeight.Bold,
                    color = BpsBlue,
                    fontSize = 14.sp
                )
                if (!trans.keterangan.isNullOrEmpty()) {
                    Text(
                        text = trans.keterangan,
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                }
                Text(
                    text = trans.tanggalBayar ?: "-",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            // BAGIAN KANAN: Tombol Aksi ATAU Label Status
            if (isPending) {
                // Jika PENDING -> Tampilkan Tombol Terima/Tolak
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onReject) {
                        Icon(Icons.Default.Close, contentDescription = "Tolak", tint = Color.Red)
                    }
                    IconButton(onClick = onAccept) {
                        Icon(Icons.Default.Check, contentDescription = "Terima", tint = BpsGreen)
                    }
                }
            } else {
                // Jika SUDAH SELESAI (VALID/REJECTED) -> Tampilkan Label
                val labelText = if (trans.statusValidasi == "VALID") "DITERIMA" else "DITOLAK"
                val labelColor = if (trans.statusValidasi == "VALID") BpsGreen else Color.Red

                Surface(
                    color = labelColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, labelColor)
                ) {
                    Text(
                        text = labelText,
                        color = labelColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// Fungsi Format Rupiah Lokal (Agar tidak error Unresolved Reference)
private fun formatRupiahLocal(number: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(number).replace("Rp", "Rp ").replace(",00", "")
}