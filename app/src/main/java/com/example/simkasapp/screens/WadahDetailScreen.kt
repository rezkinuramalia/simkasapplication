package com.example.simkasapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
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
fun WadahDetailScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("SIMKAS_PREFS", 0)
    val token = prefs.getString("TOKEN", "") ?: ""
    val role = prefs.getString("ROLE", "ANGGOTA") ?: ""

    // Ambil data wadah
    val wadah = navController.previousBackStackEntry?.savedStateHandle?.get<Kategori>("wadah")

    if (wadah == null) {
        Text("Data tidak ditemukan")
        return
    }

    // =========================================================================
    // LOGIKA PENENTU: Apakah User ini harus BAYAR atau MEMVALIDASI?
    // =========================================================================
    val isModePenyetor = if (role == "ANGGOTA") {
        true // Mahasiswa selalu mode bayar
    } else if (role == "BENDAHARA_KELAS" && wadah.level == "ANGKATAN") {
        true // Bendahara Kelas KALO buka wadah Angkatan -> Mode Bayar (Setor ke Admin)
    } else {
        false // Sisanya (Admin Angkatan, atau Bendahara buka wadah Kelas) -> Mode Validasi
    }
    // =========================================================================

    // State
    var pendingList by remember { mutableStateOf<List<TransaksiResponse>>(emptyList()) }
    var totalTerkumpulRealtime by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(false) }

    // State untuk Dialog Detail
    var showDialog by remember { mutableStateOf(false) }
    var selectedTrans by remember { mutableStateOf<TransaksiResponse?>(null) }

    fun loadData() {
        val finalToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        if (!isModePenyetor) {
            // JIKA MODE VALIDASI (Admin/Bendahara Pengelola): Ambil data pending
            isLoading = true
            RetrofitClient.instance.getPendingTransaksi(finalToken, wadah.id.toLong()).enqueue(object : Callback<List<TransaksiResponse>> {
                override fun onResponse(call: Call<List<TransaksiResponse>>, response: Response<List<TransaksiResponse>>) {
                    if (response.isSuccessful) {
                        pendingList = response.body() ?: emptyList()
                    }
                    isLoading = false
                }
                override fun onFailure(call: Call<List<TransaksiResponse>>, t: Throwable) { isLoading = false }
            })

            // Ambil Total Terkumpul Terbaru
            RetrofitClient.instance.getTotalPemasukan(finalToken, wadah.id.toLong()).enqueue(object : Callback<Double> {
                override fun onResponse(call: Call<Double>, response: Response<Double>) {
                    if(response.isSuccessful) totalTerkumpulRealtime = response.body() ?: 0.0
                }
                override fun onFailure(call: Call<Double>, t: Throwable) {}
            })
        } else {
            // JIKA MODE PENYETOR: Tidak perlu load pending list, cukup tampilkan nominal dari data awal
            totalTerkumpulRealtime = wadah.totalTerkumpul
        }
    }

    // Fungsi Validasi (Hanya dipakai jika !isModePenyetor)
    fun doValidate(idTransaksi: Long, status: String) {
        val finalToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
        RetrofitClient.instance.validasiTransaksi(finalToken, idTransaksi, status).enqueue(object : Callback<TransaksiResponse> {
            override fun onResponse(call: Call<TransaksiResponse>, response: Response<TransaksiResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, if(status=="VALID") "Diterima" else "Ditolak", Toast.LENGTH_SHORT).show()
                    showDialog = false
                    loadData()
                }
            }
            override fun onFailure(call: Call<TransaksiResponse>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    LaunchedEffect(Unit) { loadData() }

    Column(modifier = Modifier.padding(16.dp)) {
        // --- HEADER ---
        Text("Detail Pembayaran", style = MaterialTheme.typography.headlineMedium, color = BpsBlue)
        Spacer(Modifier.height(16.dp))
        Text(wadah.nama, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(wadah.keterangan ?: "-", color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        Text("Total Terkumpul: ${formatRupiahLocal(totalTerkumpulRealtime)}", fontSize = 16.sp, color = BpsGreen, fontWeight = FontWeight.Bold)

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // --- BODY (SWITCH UI BERDASARKAN MODE) ---
        if (isModePenyetor) {
            // === TAMPILAN UNTUK YANG MAU BAYAR (MHS / BENDAHARA SETOR KE ADMIN) ===
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    navController.currentBackStackEntry?.savedStateHandle?.set("selectedWadah", wadah)
                    navController.navigate("upload_form")
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BpsBlue)
            ) {
                Text("BAYAR SEKARANG")
            }
        } else {
            // === TAMPILAN UNTUK PENGELOLA (ADMIN / BENDAHARA TERIMA DARI MHS) ===
            Text("Menunggu Konfirmasi (${pendingList.size})", fontWeight = FontWeight.Bold, color = BpsBlue)
            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (pendingList.isEmpty()) {
                Text("Tidak ada pengajuan baru.", color = Color.Gray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(pendingList) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.clickable {
                                selectedTrans = item
                                showDialog = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.namaPengirim ?: "Pengirim", fontWeight = FontWeight.Bold)
                                    Text(formatRupiahLocal(item.nominal), fontSize = 14.sp)
                                    Text("Klik untuk detail & foto", fontSize = 12.sp, color = BpsBlue)
                                }
                                // Tombol Cepat
                                Row {
                                    IconButton(onClick = { doValidate(item.id, "REJECTED") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Tolak", tint = Color.Red)
                                    }
                                    IconButton(onClick = { doValidate(item.id, "VALID") }) {
                                        Icon(Icons.Default.Check, contentDescription = "Terima", tint = BpsGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG DETAIL ---
    if (showDialog && selectedTrans != null) {
        val item = selectedTrans!!
        // GANTI IP DI SINI JIKA PAKAI HP FISIK (Misal: 192.168.1.5)
        val imageUrl = "http://10.81.5.94:8080/uploads/bukti-bayar/${item.buktiBayar}"

        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Detail Pengajuan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))

                    if (item.buktiBayar != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Bukti Bayar",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) { Text("Tidak ada foto") }
                    }

                    Spacer(Modifier.height(16.dp))

                    InfoRow("Nama", item.namaPengirim ?: "-")
                    InfoRow("NIM", item.nimPengirim ?: "-")
                    InfoRow("Kelas", item.namaKelas ?: "-")
                    InfoRow("Wadah", item.namaWadah ?: "-")
                    InfoRow("Nominal", formatRupiahLocal(item.nominal))
                    InfoRow("Keterangan", item.keterangan ?: "-")
                    InfoRow("Tanggal", item.tanggalBayar ?: "-")

                    Spacer(Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(
                            onClick = { doValidate(item.id, "REJECTED") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Text("TOLAK")
                        }
                        Button(
                            onClick = { doValidate(item.id, "VALID") },
                            colors = ButtonDefaults.buttonColors(containerColor = BpsGreen),
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        ) {
                            Text("TERIMA")
                        }
                    }

                    TextButton(onClick = { showDialog = false }) {
                        Text("Tutup", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Divider(color = Color.LightGray, thickness = 0.5.dp)
    }
}

private fun formatRupiahLocal(number: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(number)
}