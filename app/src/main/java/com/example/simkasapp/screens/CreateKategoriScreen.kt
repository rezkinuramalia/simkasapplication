package com.example.simkasapp.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.simkasapp.api.RetrofitClient
import com.example.simkasapp.models.Kategori
import com.example.simkasapp.models.KategoriRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun CreateKategoriScreen(navController: NavController) {
    var namaInfo by remember { mutableStateOf("") } // Misal: "Kas Maret 2025"
    var keterangan by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current

    fun doSubmit() {
        if (namaInfo.isEmpty()) {
            Toast.makeText(context, "Nama Kas harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        val prefs = context.getSharedPreferences("SIMKAS_PREFS", 0)
        val token = prefs.getString("TOKEN", "") ?: ""

        // Kirim ke Backend
        val req = KategoriRequest(namaInfo, keterangan)

        RetrofitClient.instance.createKategori(token, req).enqueue(object : Callback<Kategori> {
            override fun onResponse(call: Call<Kategori>, response: Response<Kategori>) {
                isLoading = false
                if (response.isSuccessful) {
                    Toast.makeText(context, "Berhasil membuat Info Kas!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack() // Kembali ke Dashboard
                } else {
                    Toast.makeText(context, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Kategori>, t: Throwable) {
                isLoading = false
                Toast.makeText(context, "Error Koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Column(modifier = Modifier.padding(24.dp)) {
        Text("Buat Info Penarikan Kas", style = MaterialTheme.typography.headlineSmall)
        Text("Admin Angkatan / Bendahara", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = namaInfo,
            onValueChange = { namaInfo = it },
            label = { Text("Judul (Cth: Kas Maret 2025)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = keterangan,
            onValueChange = { keterangan = it },
            label = { Text("Keterangan (Opsional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { doSubmit() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Menyimpan..." else "SIMPAN INFO KAS")
        }
    }
}