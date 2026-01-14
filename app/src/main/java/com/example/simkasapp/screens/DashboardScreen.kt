package com.example.simkasapp.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
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
import com.example.simkasapp.models.UserDto  // Import yang Benar
import com.example.simkasapp.ui.theme.BpsBlue
import com.example.simkasapp.ui.theme.BpsOrange
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

// === ENTRY POINT UTAMA (Dipanggil dari Navigasi) ===
@Composable
fun DashboardScreenContent(token: String, role: String, navController: NavController) {
    // Container utama
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (role) {
            "ADMIN_ANGKATAN" -> {
                // Admin melihat list yang dia kelola
                DashboardListContent(token, navController, "Kelola Kas Angkatan")
            }
            "BENDAHARA_KELAS" -> {
                // Bendahara melihat list yang dia kelola
                DashboardListContent(token, navController, "Kelola Tempat Bayar Kelas")
            }
            else -> {
                // Anggota melihat welcome message saja (tidak bisa kelola)
                AnggotaDashboardContent(token)
            }
        }
    }
}

// === LOGIKA LIST PENGELOLA (ADMIN & BENDAHARA) ===
@Composable
fun DashboardListContent(token: String, navController: NavController, title: String) {
    var listWadah by remember { mutableStateOf<List<Kategori>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Fungsi fetch data (otomatis ambil yang managed by user)
    fun loadData() {
        isLoading = true
        val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        RetrofitClient.instance.getKategoriManaged(authToken).enqueue(object : Callback<List<Kategori>> {
            override fun onResponse(call: Call<List<Kategori>>, response: Response<List<Kategori>>) {
                isLoading = false
                if (response.isSuccessful) {
                    listWadah = response.body() ?: emptyList()
                } else {
                    Toast.makeText(context, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<Kategori>>, t: Throwable) {
                isLoading = false
                Toast.makeText(context, "Error koneksi", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Load saat pertama kali
    LaunchedEffect(Unit) { loadData() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BpsBlue)
        Spacer(Modifier.height(8.dp))
        Text("List ini adalah tempat bayar yang Anda buat.", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BpsOrange)
            }
        } else if (listWadah.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Belum ada tempat bayar.", color = Color.Gray)
            }
        } else {
            // Gunakan LazyColumn agar scrollable efisien
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listWadah) { wadah ->
                    WadahItemCardDashboard(
                        wadah = wadah,
                        token = token,
                        onClick = {
                            // Navigasi ke detail/validasi
                            navController.currentBackStackEntry?.savedStateHandle?.set("wadah", wadah)
                            navController.navigate("wadah_validasi_screen") // Sesuaikan route validasi Anda
                        },
                        onRefreshNeeded = {
                            loadData() // Reload agar sorting berubah (Nonaktif turun ke bawah)
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Tombol Tambah
        Button(
            onClick = { navController.navigate("create_kategori") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BpsBlue)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Buat Tempat Bayar Baru")
        }
    }
}

// === CARD ITEM DASHBOARD (Dengan Fitur Nonaktifkan) ===
@Composable
fun WadahItemCardDashboard(
    wadah: Kategori,
    token: String,
    onClick: () -> Unit,
    onRefreshNeeded: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

    // Visual: Jika aktif putih, jika nonaktif abu-abu
    val containerColor = if (wadah.aktif) Color.White else Color(0xFFF5F5F5)
    val contentColor = if (wadah.aktif) BpsBlue else Color.Gray
    val iconTint = if (wadah.aktif) BpsOrange else Color.Gray

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (wadah.aktif) 3.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Bagian Kiri (Icon + Teks)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = wadah.nama,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = contentColor
                    )
                    Text(
                        text = if ((wadah.keterangan?.length ?: 0) > 30) "${wadah.keterangan?.take(30)}..." else wadah.keterangan ?: "-",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = formatRupiah(wadah.nominal?.toDouble() ?: 0.0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                }
            }

            // Bagian Kanan (Tombol / Status)
            if (wadah.aktif) {
                // JIKA AKTIF: Tampilkan Tombol Silang (X)
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Nonaktifkan",
                        tint = Color.Red
                    )
                }
            } else {
                // JIKA NONAKTIF: Tampilkan Label Status
                Surface(
                    color = Color.Gray,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "NONAKTIF",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Dialog Konfirmasi Nonaktifkan
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nonaktifkan Wadah?") },
            text = { Text("Wadah '${wadah.nama}' akan dinonaktifkan. Mahasiswa tidak bisa membayar lagi, dan item ini akan pindah ke posisi paling bawah.") },
            confirmButton = {
                Button(
                    onClick = {
                        RetrofitClient.instance.updateStatusKategori(authToken, wadah.id, false)
                            .enqueue(object : Callback<Kategori> {
                                override fun onResponse(call: Call<Kategori>, response: Response<Kategori>) {
                                    showDialog = false
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Berhasil dinonaktifkan", Toast.LENGTH_SHORT).show()
                                        onRefreshNeeded() // TRIGGER REFRESH LIST
                                    } else {
                                        Toast.makeText(context, "Gagal update status", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                override fun onFailure(call: Call<Kategori>, t: Throwable) {
                                    showDialog = false
                                    Toast.makeText(context, "Error koneksi", Toast.LENGTH_SHORT).show()
                                }
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

// === KONTEN ANGGOTA (Hanya Welcome, tidak ada List Kelola) ===
@Composable
fun AnggotaDashboardContent(token: String) {
    var userName by remember { mutableStateOf("Mahasiswa") }

    // Ambil nama user (opsional, agar lebih personal)
    LaunchedEffect(Unit) {
        val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
        // GANTI getUserProfile -> getMyProfile
        // GANTI User -> UserDto
        RetrofitClient.instance.getMyProfile(authToken).enqueue(object : Callback<UserDto> {
            override fun onResponse(call: Call<UserDto>, response: Response<UserDto>) {
                // GANTI .name -> .nama
                if (response.isSuccessful) userName = response.body()?.nama ?: "Mahasiswa"
            }
            override fun onFailure(call: Call<UserDto>, t: Throwable) {}
        })
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Card(
            colors = CardDefaults.cardColors(containerColor = BpsBlue),
            modifier = Modifier.fillMaxWidth().height(150.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Halo, $userName!", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Selamat Datang di SimKas", color = Color.White.copy(alpha = 0.9f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Informasi Tagihan", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BpsBlue)
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = BpsOrange)
                Spacer(Modifier.width(16.dp))
                Text(
                    "Untuk melakukan pembayaran atau melihat tagihan aktif, silakan akses menu 'Bayar' di navigasi bawah.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// Helper Format Rupiah (Agar tidak error undefined)
fun formatRupiah(number: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(number).replace("Rp", "Rp ").replace(",00", "")
}