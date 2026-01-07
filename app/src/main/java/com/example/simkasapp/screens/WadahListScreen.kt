package com.example.simkasapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.example.simkasapp.ui.theme.BpsBlue
import com.example.simkasapp.ui.theme.BpsOrange
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun WadahListScreen(navController: NavController, token: String) {
    var listWadah by remember { mutableStateOf<List<Kategori>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Ambil Data Wadah (Backend sudah otomatis filter: Mhs lihat punya Bendahara, dll)
    LaunchedEffect(Unit) {
        RetrofitClient.instance.getAllKategori(token).enqueue(object : Callback<List<Kategori>> {
            override fun onResponse(call: Call<List<Kategori>>, response: Response<List<Kategori>>) {
                isLoading = false
                if (response.isSuccessful) {
                    // Sort dari terbaru (id terbesar biasanya terbaru)
                    listWadah = response.body()?.sortedByDescending { it.id } ?: emptyList()
                }
            }
            override fun onFailure(call: Call<List<Kategori>>, t: Throwable) { isLoading = false }
        })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tagihan & Tempat Bayar", style = MaterialTheme.typography.headlineSmall, color = BpsBlue, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (listWadah.isEmpty()) {
            Text("Belum ada tagihan aktif.", color = Color.Gray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listWadah) { wadah ->
                    WadahItemCard(wadah) {
                        // KLIK -> Masuk ke Detail & Bayar
                        // Kita kirim data wadah lewat route argument (simplifikasi)
                        navController.currentBackStackEntry?.savedStateHandle?.set("wadah", wadah)
                        navController.navigate("wadah_detail")
                    }
                }
            }
        }
    }
}

@Composable
fun WadahItemCard(wadah: Kategori, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Icon Surat/Notif
            Icon(Icons.Default.Notifications, contentDescription = null, tint = BpsOrange, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(wadah.nama, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BpsBlue)
                Text(
                    text = if ((wadah.keterangan?.length ?: 0) > 30) "${wadah.keterangan?.take(30)}..." else wadah.keterangan ?: "",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}