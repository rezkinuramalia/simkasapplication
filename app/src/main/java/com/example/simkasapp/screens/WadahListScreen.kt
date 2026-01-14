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

    LaunchedEffect(Unit) {
        val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        // PANGGIL ENDPOINT 'PAYMENT' (Yang harus dibayar)
        // Backend sudah mengurutkan: Aktif paling atas, Nonaktif paling bawah
        RetrofitClient.instance.getKategoriPayment(authToken).enqueue(object : Callback<List<Kategori>> {
            override fun onResponse(call: Call<List<Kategori>>, response: Response<List<Kategori>>) {
                isLoading = false
                if (response.isSuccessful) {
                    listWadah = response.body() ?: emptyList()
                }
            }
            override fun onFailure(call: Call<List<Kategori>>, t: Throwable) { isLoading = false }
        })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Daftar Tagihan", style = MaterialTheme.typography.headlineSmall, color = BpsBlue, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (listWadah.isEmpty()) {
            Text("Tidak ada tagihan saat ini.", color = Color.Gray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listWadah) { wadah ->
                    WadahItemCard(wadah) {
                        // LOGIKA KLIK:
                        // Hanya bisa diklik jika AKTIF
                        if (wadah.aktif) {
                            navController.currentBackStackEntry?.savedStateHandle?.set("wadah", wadah)
                            navController.navigate("wadah_detail")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WadahItemCard(wadah: Kategori, onClick: () -> Unit) {
    Card(
        // Visual Feedback: Abu-abu jika nonaktif
        colors = CardDefaults.cardColors(containerColor = if(wadah.aktif) Color.White else Color(0xFFEEEEEE)),
        elevation = CardDefaults.cardElevation(defaultElevation = if(wadah.aktif) 4.dp else 0.dp),
        // Disable Click jika nonaktif
        modifier = Modifier.fillMaxWidth().clickable(enabled = wadah.aktif) { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = if(wadah.aktif) BpsOrange else Color.Gray,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(wadah.nama, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if(wadah.aktif) BpsBlue else Color.Gray)
                Text(
                    text = "Rp ${String.format("%,.0f", wadah.nominal)}",
                    fontWeight = FontWeight.Bold,
                    color = if(wadah.aktif) BpsBlue else Color.Gray,
                    fontSize = 14.sp
                )
            }

            // Indikator Status
            if (!wadah.aktif) {
                Surface(color = Color.Gray, shape = MaterialTheme.shapes.small) {
                    Text("NONAKTIF", modifier = Modifier.padding(4.dp), fontSize = 10.sp, color = Color.White)
                }
            } else {
                Surface(
                    color = if(wadah.level == "ANGKATAN") Color(0xFFE3F2FD) else Color(0xFFF1F8E9),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = wadah.level ?: "KELAS",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if(wadah.level == "ANGKATAN") Color.Blue else Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}