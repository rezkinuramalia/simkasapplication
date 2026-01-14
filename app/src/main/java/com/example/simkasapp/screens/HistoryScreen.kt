package com.example.simkasapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.simkasapp.api.RetrofitClient
import com.example.simkasapp.models.HistoryTransaksi
import com.example.simkasapp.ui.theme.BpsBlue
import com.example.simkasapp.ui.theme.BpsGreen
import com.example.simkasapp.ui.theme.PastelRed
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HistoryScreen(navController: NavController, token: String) {
    var listHistory by remember { mutableStateOf<List<HistoryTransaksi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Mengambil LiveData dari savedStateHandle navigasi untuk refresh otomatis
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val refreshTrigger by savedStateHandle?.getLiveData<Boolean>("refresh_history")
        ?.observeAsState(false)
        ?: remember { mutableStateOf(false) }

    fun loadData() {
        isLoading = true
        val finalToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        RetrofitClient.instance.getMyHistory(finalToken).enqueue(object : Callback<List<HistoryTransaksi>> {
            override fun onResponse(call: Call<List<HistoryTransaksi>>, response: Response<List<HistoryTransaksi>>) {
                isLoading = false
                if (response.isSuccessful) {
                    listHistory = response.body() ?: emptyList()
                }
            }
            override fun onFailure(call: Call<List<HistoryTransaksi>>, t: Throwable) {
                isLoading = false
            }
        })
    }

    // Load data pertama kali
    LaunchedEffect(Unit) { loadData() }

    // Load ulang data jika ada trigger refresh
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger) {
            loadData()
            navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("refresh_history")
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Riwayat Transaksi", style = MaterialTheme.typography.headlineSmall, color = BpsBlue, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (listHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada riwayat transaksi.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listHistory) { item ->
                    HistoryItemCard(item)
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(item: HistoryTransaksi) {
    val colorStatus = when (item.statusValidasi) {
        "VALID" -> BpsGreen
        "REJECTED" -> PastelRed
        else -> Color(0xFFFFA500) // Orange untuk Pending
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Baris Atas: Nama Wadah & Status Label
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = item.namaWadah ?: "Pembayaran Kas",
                    fontWeight = FontWeight.Bold,
                    color = BpsBlue,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = item.statusValidasi ?: "PENDING",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(colorStatus, shape = MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Info Detail
            Text("Nominal: ${formatRupiahLocal(item.nominal)}", fontWeight = FontWeight.SemiBold)
            Text("Ket: ${item.keterangan ?: "-"}", fontSize = 12.sp, color = Color.Gray)
            Text("Tgl: ${item.tanggalBayar ?: "-"}", fontSize = 12.sp, color = Color.Gray)

            // [LOGIKA TAMPILAN CATATAN PENOLAKAN]
            if (item.statusValidasi == "REJECTED" && !item.catatanAdmin.isNullOrEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)

                Text(
                    text = "Alasan Penolakan:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
                Text(
                    text = item.catatanAdmin,
                    color = Color.Red,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

// Helper format rupiah private agar tidak bentrok
private fun formatRupiahLocal(number: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(number)
}