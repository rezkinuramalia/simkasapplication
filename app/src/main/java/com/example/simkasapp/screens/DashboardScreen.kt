package com.example.simkasapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.simkasapp.api.RetrofitClient
import com.example.simkasapp.models.DashboardAngkatanResponse
import com.example.simkasapp.models.DashboardKelasResponse
import com.example.simkasapp.ui.theme.BpsBlue
import com.example.simkasapp.ui.theme.BpsGreen
import com.example.simkasapp.ui.theme.BpsOrange
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

// --- FUNGSI BARU: CONTENT ONLY (Tanpa Scaffold/TopBar) ---
// Ini yang dipanggil oleh MainContainerScreen agar tidak double Scaffold
@Composable
fun DashboardScreenContent(token: String, role: String, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // TAMPILAN BERDASARKAN ROLE
        when (role) {
            "ADMIN_ANGKATAN" -> AdminDashboardContent(token, navController)
            "BENDAHARA_KELAS" -> BendaharaDashboardContent(token, navController)
            else -> AnggotaDashboardContent(token, navController, "Mahasiswa")
        }
    }
}

// --- FUNGSI LAMA (WRAPPER) ---
// Tetap dipertahankan kalau ada yang manggil DashboardScreen secara langsung (misal testing)
@Composable
fun DashboardScreen(navController: NavController) {
    // Kosong atau redirect jika diperlukan, tapi sebaiknya gunakan MainContainerScreen
}

// === 1. KONTEN ADMIN ANGKATAN ===
@Composable
fun AdminDashboardContent(token: String, navController: NavController) {
    var data by remember { mutableStateOf<DashboardAngkatanResponse?>(null) }

    LaunchedEffect(Unit) {
        RetrofitClient.instance.getDashboardAngkatan(token).enqueue(object : Callback<DashboardAngkatanResponse> {
            override fun onResponse(call: Call<DashboardAngkatanResponse>, response: Response<DashboardAngkatanResponse>) {
                if(response.isSuccessful) data = response.body()
            }
            override fun onFailure(call: Call<DashboardAngkatanResponse>, t: Throwable) {}
        })
    }

    Column {
        Text("Statistik Angkatan", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BpsBlue)
        Spacer(Modifier.height(16.dp))

        if (data != null) {
            StatCard("Saldo Angkatan", formatRupiah(data!!.saldoKas), BpsBlue, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                StatCard("Pemasukan", formatRupiah(data!!.totalPemasukan), BpsGreen, Modifier.weight(1f))
                StatCard("Pengeluaran", formatRupiah(data!!.totalPengeluaran), BpsOrange, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            StatCard("Menunggu Validasi", "${data!!.totalTransaksiPending} Transaksi", Color.Red, Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(24.dp))
        Text("Aksi Cepat", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { navController.navigate("create_kategori") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BpsBlue)
        ) {
            Text("Buat Tempat Penarikan (Wadah)")
        }
    }
}

// === 2. KONTEN BENDAHARA KELAS ===
@Composable
fun BendaharaDashboardContent(token: String, navController: NavController) {
    var data by remember { mutableStateOf<DashboardKelasResponse?>(null) }

    LaunchedEffect(Unit) {
        RetrofitClient.instance.getDashboardKelas(token).enqueue(object : Callback<DashboardKelasResponse> {
            override fun onResponse(call: Call<DashboardKelasResponse>, response: Response<DashboardKelasResponse>) {
                if(response.isSuccessful) data = response.body()
            }
            override fun onFailure(call: Call<DashboardKelasResponse>, t: Throwable) {}
        })
    }

    Column {
        Text("Statistik Kelas", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BpsBlue)
        Spacer(Modifier.height(16.dp))

        if (data != null) {
            StatCard("Saldo Kelas", formatRupiah(data!!.saldoKas), BpsBlue, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                StatCard("Pemasukan", formatRupiah(data!!.totalPemasukan), BpsGreen, Modifier.weight(1f))
                StatCard("Pengeluaran", formatRupiah(data!!.totalPengeluaran), BpsOrange, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                StatCard("Belum Bayar", "${data!!.anggotaBelumBayarBulanIni} Orang", Color.Red, Modifier.weight(1f))
                StatCard("Pending", "${data!!.transaksiPending}", BpsOrange, Modifier.weight(1f))
            }
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(24.dp))
        Text("Aksi Cepat", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { navController.navigate("create_kategori") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BpsBlue)
        ) {
            Text("Buat Tagihan Kelas")
        }
    }
}

// === 3. KONTEN ANGGOTA ===
@Composable
fun AnggotaDashboardContent(token: String, navController: NavController, username: String?) {
    Column {
        Card(
            colors = CardDefaults.cardColors(containerColor = BpsBlue),
            modifier = Modifier.fillMaxWidth().height(120.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Halo, ${username ?: "Mahasiswa"}!", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Selamat Datang di SimKas", color = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Info Tagihan", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Silakan cek menu 'Bayar' di bawah untuk melihat daftar iuran/kas yang tersedia.", color = Color.Gray)
            }
        }
    }
}

// Helper UI
@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = modifier.padding(4.dp).height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.White, fontSize = 12.sp)
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun formatRupiah(number: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(number)
}