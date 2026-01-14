package com.example.simkasapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
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
import com.example.simkasapp.models.Kategori
import com.example.simkasapp.ui.theme.BpsBlue
import com.example.simkasapp.ui.theme.BpsGreen
import com.example.simkasapp.ui.theme.BpsOrange
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreenContent(token: String, role: String, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        when (role) {
            "ADMIN_ANGKATAN" -> AdminDashboardContent(token, navController)
            "BENDAHARA_KELAS" -> BendaharaDashboardContent(token, navController)
            else -> AnggotaDashboardContent(token, navController, "Mahasiswa")
        }
    }
}

// === 1. KONTEN ADMIN ANGKATAN ===
@Composable
fun AdminDashboardContent(token: String, navController: NavController) {
    DashboardListContent(token, navController, "Kelola Kas Angkatan")
}

// === 2. KONTEN BENDAHARA KELAS ===
@Composable
fun BendaharaDashboardContent(token: String, navController: NavController) {
    DashboardListContent(token, navController, "Kelola Tempat Bayar Kelas")
}

// Komponen Reusable untuk List Dashboard (Admin & Bendahara)
@Composable
fun DashboardListContent(token: String, navController: NavController, title: String) {
    var listWadah by remember { mutableStateOf<List<Kategori>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadData() {
        isLoading = true
        // PANGGIL ENDPOINT 'MANAGED' (Yang dibuat sendiri)
        RetrofitClient.instance.getKategoriManaged(token).enqueue(object : Callback<List<Kategori>> {
            override fun onResponse(call: Call<List<Kategori>>, response: Response<List<Kategori>>) {
                isLoading = false
                if (response.isSuccessful) listWadah = response.body() ?: emptyList()
            }
            override fun onFailure(call: Call<List<Kategori>>, t: Throwable) { isLoading = false }
        })
    }

    LaunchedEffect(Unit) { loadData() }

    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BpsBlue)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else if (listWadah.isEmpty()) {
            Text("Belum ada tempat bayar yang dibuat.", color = Color.Gray)
        } else {
            listWadah.forEach { wadah ->
                WadahItemCardDashboard(wadah, token,
                    onClick = {
                        navController.currentBackStackEntry?.savedStateHandle?.set("wadah", wadah)
                        navController.navigate("wadah_validasi_screen") // Atau detail
                    },
                    onRefresh = { loadData() } // Refresh list setelah update status
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { navController.navigate("create_kategori") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BpsBlue)
        ) {
            Text("Buat Tempat Bayar Baru")
        }
    }
}

@Composable
fun WadahItemCardDashboard(
    wadah: Kategori,
    token: String,
    onClick: () -> Unit,
    onRefresh: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = if (wadah.aktif) Color.White else Color(0xFFEEEEEE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if(wadah.aktif) BpsOrange else Color.Gray,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(wadah.nama, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if(wadah.aktif) BpsBlue else Color.Gray)
                    Text(
                        text = if ((wadah.keterangan?.length ?: 0) > 30) "${wadah.keterangan?.take(30)}..." else wadah.keterangan ?: "-",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            // Tombol Nonaktifkan / Status
            if (wadah.aktif) {
                IconButton(onClick = { showDialog = true }) {
                    Icon(painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_close_clear_cancel), contentDescription = "Nonaktifkan", tint = Color.Red)
                }
            } else {
                Surface(color = Color.Gray, shape = MaterialTheme.shapes.small) {
                    Text("NONAKTIF", modifier = Modifier.padding(4.dp), fontSize = 10.sp, color = Color.White)
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nonaktifkan Wadah?") },
            text = { Text("Wadah ini tidak akan bisa menerima pembayaran lagi. Data history tetap aman.") },
            confirmButton = {
                Button(
                    onClick = {
                        RetrofitClient.instance.updateStatusKategori(token, wadah.id, false)
                            .enqueue(object : Callback<Kategori> {
                                override fun onResponse(call: Call<Kategori>, response: Response<Kategori>) {
                                    showDialog = false
                                    onRefresh() // Refresh list
                                }
                                override fun onFailure(call: Call<Kategori>, t: Throwable) {}
                            })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Nonaktifkan") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Batal") }
            }
        )
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
                Text("Silakan cek menu 'Bayar' di bawah untuk melihat tagihan iuran kelas.", color = Color.Gray)
            }
        }
    }
}

// --- KOMPONEN UI TAMBAHAN ---

@Composable
fun WadahItemCardDashboard(wadah: Kategori, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = BpsOrange,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(wadah.nama, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BpsBlue)
                Text(
                    text = if ((wadah.keterangan?.length ?: 0) > 35) "${wadah.keterangan?.take(35)}..." else wadah.keterangan ?: "-",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = modifier
            .padding(4.dp)
            .height(100.dp)
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

// Fungsi ini dibuat Publik agar bisa dipakai di screen lain (seperti WadahValidasiScreen)
fun formatRupiah(number: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(number)
}