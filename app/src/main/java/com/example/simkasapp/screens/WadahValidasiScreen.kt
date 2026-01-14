package com.example.simkasapp.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun WadahValidasiScreen(navController: NavController, token: String, wadah: Kategori) {
    var listData by remember { mutableStateOf<List<TransaksiResponse>>(emptyList()) }
    var totalMasuk by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }

    // State untuk Dialog Detail & Reject
    var showDetailDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var selectedTrans by remember { mutableStateOf<TransaksiResponse?>(null) }

    val context = LocalContext.current

    fun loadData() {
        isLoading = true
        val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        RetrofitClient.instance.getPendingTransaksi(authToken, wadah.id).enqueue(object : Callback<List<TransaksiResponse>> {
            override fun onResponse(call: Call<List<TransaksiResponse>>, response: Response<List<TransaksiResponse>>) {
                if (response.isSuccessful) {
                    listData = response.body() ?: emptyList()
                }
                isLoading = false
            }
            override fun onFailure(call: Call<List<TransaksiResponse>>, t: Throwable) {
                isLoading = false
                t.printStackTrace()
            }
        })

        RetrofitClient.instance.getTotalPemasukan(authToken, wadah.id).enqueue(object : Callback<Double> {
            override fun onResponse(call: Call<Double>, response: Response<Double>) {
                if (response.isSuccessful) totalMasuk = response.body() ?: 0.0
            }
            override fun onFailure(call: Call<Double>, t: Throwable) {}
        })
    }

    LaunchedEffect(Unit) { loadData() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(wadah.nama, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = BpsBlue)
        Text("Total Pemasukan: ${formatRupiahLocal(totalMasuk)}", color = BpsGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)

        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Daftar Pengajuan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Klik kartu untuk melihat bukti bayar & detail", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (listData.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Belum ada data transaksi.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listData) { trans ->
                    TransactionCardItem(
                        trans = trans,
                        onCardClick = {
                            selectedTrans = trans
                            showDetailDialog = true
                        },
                        onAccept = {
                            doValidasi(context, token, trans.id, "VALID", null) { loadData() }
                        },
                        onRejectClick = {
                            selectedTrans = trans
                            showRejectDialog = true
                        }
                    )
                }
            }
        }
    }

    // === DIALOG DETAIL TRANSAKSI (Lihat Bukti & Isi Form) ===
    if (showDetailDialog && selectedTrans != null) {
        DetailTransactionDialog(
            trans = selectedTrans!!,
            onDismiss = { showDetailDialog = false }
        )
    }

    // === DIALOG TOLAK (Isi Catatan) ===
    if (showRejectDialog && selectedTrans != null) {
        RejectReasonDialog(
            onDismiss = { showRejectDialog = false },
            onConfirm = { reason ->
                doValidasi(context, token, selectedTrans!!.id, "REJECTED", reason) {
                    loadData()
                }
                showRejectDialog = false
            }
        )
    }
}

// --- FUNGSI VALIDASI ---
fun doValidasi(context: android.content.Context, token: String, id: Long, status: String, note: String?, onSuccess: () -> Unit) {
    val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

    // API memanggil validasiTransaksi dengan parameter status & catatan
    RetrofitClient.instance.validasiTransaksi(authToken, id, status, note).enqueue(object : Callback<TransaksiResponse> {
        override fun onResponse(call: Call<TransaksiResponse>, response: Response<TransaksiResponse>) {
            if (response.isSuccessful) {
                val msg = if (status == "VALID") "Pengajuan Diterima" else "Pengajuan Ditolak"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onSuccess()
            } else {
                Toast.makeText(context, "Gagal memproses validasi", Toast.LENGTH_SHORT).show()
            }
        }
        override fun onFailure(call: Call<TransaksiResponse>, t: Throwable) {
            Toast.makeText(context, "Error koneksi", Toast.LENGTH_SHORT).show()
        }
    })
}

// --- KOMPONEN CARD TRANSAKSI ---
@Composable
fun TransactionCardItem(
    trans: TransaksiResponse,
    onCardClick: () -> Unit,
    onAccept: () -> Unit,
    onRejectClick: () -> Unit
) {
    val isPending = (trans.statusValidasi == "PENDING")

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() } // Klik kartu untuk lihat detail
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(trans.namaPengirim ?: "Mahasiswa", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(trans.nimPengirim ?: "-", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                Text(formatRupiahLocal(trans.nominal), fontWeight = FontWeight.Bold, color = BpsBlue, fontSize = 14.sp)

                if (!trans.keterangan.isNullOrEmpty()) {
                    Text(trans.keterangan, fontSize = 12.sp, color = Color.Black, maxLines = 1)
                }

                // Indikator kecil "Ketuk untuk detail"
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Lihat Detail", fontSize = 10.sp, color = Color.Gray)
                }
            }

            // Tombol Aksi / Status
            if (isPending) {
                Row {
                    IconButton(onClick = onRejectClick) {
                        Icon(Icons.Default.Close, contentDescription = "Tolak", tint = Color.Red)
                    }
                    IconButton(onClick = onAccept) {
                        Icon(Icons.Default.Check, contentDescription = "Terima", tint = BpsGreen)
                    }
                }
            } else {
                val labelText = if (trans.statusValidasi == "VALID") "DITERIMA" else "DITOLAK"
                val labelColor = if (trans.statusValidasi == "VALID") BpsGreen else Color.Red
                Surface(
                    color = labelColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, labelColor)
                ) {
                    Text(labelText, color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                }
            }
        }
    }
}

// --- DIALOG DETAIL (Menampilkan Bukti Bayar) ---
@Composable
fun DetailTransactionDialog(trans: TransaksiResponse, onDismiss: () -> Unit) {
    // URL Bukti Bayar (Sesuaikan IP jika perlu, misal pakai BuildConfig di real app)
    // Asumsi: Backend menyimpan file di folder uploads/bukti-bayar yang bisa diakses statis
    val imageUrl = "http://10.0.2.2:8080/uploads/bukti-bayar/${trans.buktiBayar}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detail Pengajuan", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Tampilan Foto Bukti Bayar
                if (trans.buktiBayar != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Bukti Bayar",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(8.dp))
                } else {
                    Box(Modifier.fillMaxWidth().height(150.dp).background(Color.LightGray), contentAlignment = Alignment.Center) {
                        Text("Tidak ada bukti bayar", fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
                DetailRow("Nama", trans.namaPengirim ?: "-")
                DetailRow("NIM", trans.nimPengirim ?: "-")
                DetailRow("Tanggal", trans.tanggalBayar ?: "-")
                DetailRow("Keterangan", trans.keterangan ?: "-")

                if (trans.catatanAdmin != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("Catatan Admin:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Red)
                    Text(trans.catatanAdmin, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = BpsBlue)) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.width(80.dp))
        Text(": $value", fontSize = 12.sp)
    }
}

// --- DIALOG ALASAN PENOLAKAN ---
@Composable
fun RejectReasonDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tolak Pengajuan") },
        text = {
            Column {
                Text("Berikan alasan penolakan agar pengirim tahu kekurangannya:", fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Alasan Penolakan") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                enabled = reason.isNotEmpty() // Harus isi alasan
            ) {
                Text("Tolak & Kirim")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

private fun formatRupiahLocal(number: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(number).replace("Rp", "Rp ").replace(",00", "")
}