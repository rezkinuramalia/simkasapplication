package com.example.simkasapp.screens

import android.util.Log
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
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        RetrofitClient.instance.getAllKategori(authToken).enqueue(object : Callback<List<Kategori>> {
            override fun onResponse(call: Call<List<Kategori>>, response: Response<List<Kategori>>) {
                isLoading = false
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("SIMKAS_DEBUG", "Data Berhasil Masuk: ${body?.size} item")
                    listWadah = body?.sortedByDescending { it.id } ?: emptyList()
                } else {
                    errorMessage = "Gagal: ${response.code()}"
                    Log.e("SIMKAS_DEBUG", "Error API: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<List<Kategori>>, t: Throwable) {
                isLoading = false
                errorMessage = "Koneksi Gagal"
                Log.e("SIMKAS_DEBUG", "Fatal Error", t)
            }
        })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tagihan & Tempat Bayar", style = MaterialTheme.typography.headlineSmall, color = BpsBlue, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BpsBlue)
            }
        } else if (errorMessage != null) {
            Text(errorMessage!!, color = Color.Red)
        } else if (listWadah.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Daftar Kosong. Belum ada kategori yang dibuat.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listWadah) { wadah ->
                    WadahItemCard(wadah) {
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Gunakan icon Notifications (Pasti Ada di Library Default)
            Icon(Icons.Default.Notifications, contentDescription = null, tint = BpsOrange, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(wadah.nama, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BpsBlue)
                Text(wadah.keterangan ?: "-", fontSize = 13.sp, color = Color.Gray)

                // Menampilkan Nominal
                Text(
                    text = "Rp ${String.format("%,.0f", wadah.nominal ?: 0.0)}",
                    fontWeight = FontWeight.Bold, color = BpsBlue, fontSize = 14.sp
                )
            }

            // PERBAIKAN ERROR: Tambahkan ?: "" agar tidak null
            Surface(
                color = if((wadah.level ?: "") == "ANGKATAN") Color(0xFFE3F2FD) else Color(0xFFF1F8E9),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = wadah.level ?: "KELAS", // Perbaikan Argument Mismatch
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if((wadah.level ?: "") == "ANGKATAN") Color.Blue else Color(0xFF2E7D32)
                )
            }
        }
    }
}