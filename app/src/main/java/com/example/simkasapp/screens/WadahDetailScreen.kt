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
import androidx.compose.ui.text.font.FontStyle
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

    // Ambil data wadah dari navigasi
    val wadah = navController.previousBackStackEntry?.savedStateHandle?.get<Kategori>("wadah")

    if (wadah == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Data tidak ditemukan")
        }
        return
    }

    // --- 1. LOGIKA MODE TAMPILAN (Bayar vs Validasi) ---
    val isModePenyetor = if (role == "ANGGOTA") {
        true
    } else if (role == "BENDAHARA_KELAS" && wadah.level == "ANGKATAN") {
        true
    } else {
        false
    }

    // --- 2. STATE VARIABLES ---
    var listTransaksi by remember { mutableStateOf<List<TransaksiResponse>>(emptyList()) }
    var totalTerkumpulRealtime by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(false) }

    // State untuk Dialog Detail
    var showDialog by remember { mutableStateOf(false) }
    var selectedTrans by remember { mutableStateOf<TransaksiResponse?>(null) }

    // State untuk Dialog Penolakan (Catatan)
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var transactionToRejectId by remember { mutableStateOf<Long?>(null) }

    // --- 3. FUNGSI LOAD DATA ---
    fun loadData() {
        val finalToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        if (!isModePenyetor) {
            isLoading = true
            // Endpoint ini mengembalikan list yang sudah disortir Backend (Pending di atas)
            RetrofitClient.instance.getPendingTransaksi(finalToken, wadah.id.toLong()).enqueue(object : Callback<List<TransaksiResponse>> {
                override fun onResponse(call: Call<List<TransaksiResponse>>, response: Response<List<TransaksiResponse>>) {
                    if (response.isSuccessful) {
                        listTransaksi = response.body() ?: emptyList()
                    }
                    isLoading = false
                }
                override fun onFailure(call: Call<List<TransaksiResponse>>, t: Throwable) { isLoading = false }
            })

            // Update Total Uang
            RetrofitClient.instance.getTotalPemasukan(finalToken, wadah.id.toLong()).enqueue(object : Callback<Double> {
                override fun onResponse(call: Call<Double>, response: Response<Double>) {
                    if(response.isSuccessful) totalTerkumpulRealtime = response.body() ?: 0.0
                }
                override fun onFailure(call: Call<Double>, t: Throwable) {}
            })
        } else {
            totalTerkumpulRealtime = wadah.totalTerkumpul
        }
    }

    // --- 4. FUNGSI VALIDASI (Terima/Tolak) ---
    fun doValidate(idTransaksi: Long, status: String, catatan: String? = null) {
        val finalToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        RetrofitClient.instance.validasiTransaksi(finalToken, idTransaksi, status, catatan)
            .enqueue(object : Callback<TransaksiResponse> {
                override fun onResponse(call: Call<TransaksiResponse>, response: Response<TransaksiResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, if(status=="VALID") "Diterima" else "Ditolak", Toast.LENGTH_SHORT).show()
                        showDialog = false       // Tutup dialog detail
                        showRejectDialog = false // Tutup dialog tolak
                        loadData()               // Refresh list agar urutan terupdate
                    }
                }
                override fun onFailure(call: Call<TransaksiResponse>, t: Throwable) {
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Load data saat layar dibuka
    LaunchedEffect(Unit) { loadData() }

    // --- 5. UI UTAMA ---
    Column(modifier = Modifier.padding(16.dp)) {
        // Header
        Text("Detail Pembayaran", style = MaterialTheme.typography.headlineMedium, color = BpsBlue)
        Spacer(Modifier.height(16.dp))
        Text(wadah.nama, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(wadah.keterangan ?: "-", color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        Text("Total Terkumpul: ${formatRupiahLocal(totalTerkumpulRealtime)}", fontSize = 16.sp, color = BpsGreen, fontWeight = FontWeight.Bold)

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        if (isModePenyetor) {
            // === TAMPILAN PENYETOR (Tombol Bayar) ===
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
            // === TAMPILAN VALIDATOR (List Pengajuan) ===
            Text("Daftar Konfirmasi (${listTransaksi.size})", fontWeight = FontWeight.Bold, color = BpsBlue)
            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (listTransaksi.isEmpty()) {
                Text("Tidak ada pengajuan.", color = Color.Gray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listTransaksi) { item ->
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

                                    if (item.statusValidasi != "PENDING") {
                                        // Jika sudah diproses, tampilkan catatan kecil jika ada
                                        if (item.statusValidasi == "REJECTED" && !item.catatanAdmin.isNullOrEmpty()) {
                                            Text("Catatan: ${item.catatanAdmin}", fontSize = 11.sp, color = Color.Red, fontStyle = FontStyle.Italic)
                                        } else {
                                            Text("Selesai", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    } else {
                                        Text("Klik untuk detail & foto", fontSize = 12.sp, color = BpsBlue)
                                    }
                                }

                                // === AKSI DI LIST ITEM ===
                                if (item.statusValidasi == "PENDING") {
                                    // Tombol Cepat: Tolak & Terima
                                    Row {
                                        IconButton(onClick = {
                                            // Buka Dialog Alasan Penolakan
                                            transactionToRejectId = item.id.toLong()
                                            rejectReason = ""
                                            showRejectDialog = true
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Tolak", tint = Color.Red)
                                        }
                                        IconButton(onClick = {
                                            // Terima Langsung
                                            doValidate(item.id.toLong(), "VALID", "Pembayaran Diterima")
                                        }) {
                                            Icon(Icons.Default.Check, contentDescription = "Terima", tint = BpsGreen)
                                        }
                                    }
                                } else {
                                    // Label Status (VALID/DITOLAK)
                                    val bgColor = if (item.statusValidasi == "VALID") BpsGreen else Color.Red
                                    val label = if (item.statusValidasi == "VALID") "VALID" else "DITOLAK"
                                    Box(
                                        modifier = Modifier
                                            .background(bgColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 6. DIALOG DETAIL TRANSAKSI ---
    if (showDialog && selectedTrans != null) {
        val item = selectedTrans!!
        // [PENTING] Mengambil Base URL dinamis dari RetrofitClient agar gambar tidak error
        val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")
        val imageUrl = "$baseUrl/uploads/bukti-bayar/${item.buktiBayar}"

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
                    InfoRow("Status", item.statusValidasi ?: "PENDING")

                    if (item.statusValidasi == "REJECTED" && !item.catatanAdmin.isNullOrEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Alasan Penolakan:", fontWeight = FontWeight.Bold, color = Color.Red)
                        Text(item.catatanAdmin, color = Color.Red)
                    }

                    Spacer(Modifier.height(24.dp))

                    // Tombol Aksi di Dialog (Hanya jika PENDING)
                    if (item.statusValidasi == "PENDING") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button(
                                onClick = {
                                    // Buka Dialog Penolakan dari sini juga
                                    transactionToRejectId = item.id.toLong()
                                    rejectReason = ""
                                    showRejectDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Text("TOLAK")
                            }
                            Button(
                                onClick = { doValidate(item.id.toLong(), "VALID", "Pembayaran Diterima") },
                                colors = ButtonDefaults.buttonColors(containerColor = BpsGreen),
                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                            ) {
                                Text("TERIMA")
                            }
                        }
                    } else {
                        Text("Transaksi ini sudah diproses.", color = Color.Gray)
                    }

                    TextButton(onClick = { showDialog = false }) {
                        Text("Tutup", color = Color.Gray)
                    }
                }
            }
        }
    }

    // --- 7. DIALOG INPUT ALASAN PENOLAKAN ---
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Alasan Penolakan") },
            text = {
                Column {
                    Text("Tuliskan alasan penolakan:", fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        placeholder = { Text("Contoh: Bukti buram / Nominal salah") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (transactionToRejectId != null) {
                            doValidate(transactionToRejectId!!, "REJECTED", rejectReason)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Kirim Penolakan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Batal")
                }
            }
        )
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