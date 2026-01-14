package com.example.simkasapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
    var listPending by remember { mutableStateOf<List<TransaksiResponse>>(emptyList()) }
    var totalMasuk by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadData() {
        isLoading = true
        // 1. Ambil List Pending
        RetrofitClient.instance.getPendingTransaksi(token, wadah.id).enqueue(object : Callback<List<TransaksiResponse>> {
            override fun onResponse(call: Call<List<TransaksiResponse>>, response: Response<List<TransaksiResponse>>) {
                if (response.isSuccessful) {
                    listPending = response.body() ?: emptyList()
                }
                isLoading = false
            }
            override fun onFailure(call: Call<List<TransaksiResponse>>, t: Throwable) { isLoading = false }
        })

        // 2. Ambil Total Pemasukan Valid
        RetrofitClient.instance.getTotalPemasukan(token, wadah.id).enqueue(object : Callback<Double> {
            override fun onResponse(call: Call<Double>, response: Response<Double>) {
                if (response.isSuccessful) {
                    totalMasuk = response.body() ?: 0.0
                }
            }
            override fun onFailure(call: Call<Double>, t: Throwable) {}
        })
    }

    LaunchedEffect(Unit) { loadData() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header Wadah
        Text(wadah.nama, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = BpsBlue)

        // [PERBAIKAN 1] Gunakan formatRupiahLocal agar tidak error
        Text("Total Pemasukan: ${formatRupiahLocal(totalMasuk)}", color = BpsGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)

        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Menunggu Konfirmasi (${listPending.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (listPending.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                Text("Tidak ada pengajuan baru.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listPending) { trans ->
                    PendingTransactionCard(
                        trans = trans,
                        onAccept = {
                            doValidasi(token, trans.id, "VALID") { loadData() }
                        },
                        onReject = {
                            doValidasi(token, trans.id, "REJECTED") { loadData() }
                        }
                    )
                }
            }
        }
    }
}

fun doValidasi(token: String, id: Long, status: String, onSuccess: () -> Unit) {
    RetrofitClient.instance.validasiTransaksi(token, id, status, null).enqueue(object : Callback<TransaksiResponse> {
        override fun onResponse(call: Call<TransaksiResponse>, response: Response<TransaksiResponse>) {
            if (response.isSuccessful) onSuccess()
        }
        override fun onFailure(call: Call<TransaksiResponse>, t: Throwable) {}
    })
}

@Composable
fun PendingTransactionCard(trans: TransaksiResponse, onAccept: () -> Unit, onReject: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // [PERBAIKAN 2] Gunakan formatRupiahLocal
                Text(
                    text = "Nominal: ${formatRupiahLocal(trans.nominal)}",
                    fontWeight = FontWeight.Bold,
                    color = BpsBlue
                )
                Text(
                    text = trans.keterangan ?: "-",
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Text(
                    text = trans.tanggalBayar ?: "-",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onReject) {
                    Icon(Icons.Default.Close, contentDescription = "Tolak", tint = Color.Red, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onAccept) {
                    Icon(Icons.Default.Check, contentDescription = "Terima", tint = BpsGreen, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

// [SOLUSI] Tambahkan fungsi format rupiah private di sini agar tidak unresolved reference
private fun formatRupiahLocal(number: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(number)
}