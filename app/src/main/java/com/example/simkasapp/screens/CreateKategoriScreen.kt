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

        val prefs = context.getSharedPreferences("SIMKAS_PREFS", 0)
        val token = prefs.getString("TOKEN", null) // Default null untuk cek apakah ada

        // 🔍 DIAGNOSA 1: Cek apakah token benar-benar ada?
        if (token.isNullOrEmpty()) {
            Toast.makeText(context, "FATAL: Token Kosong! Silakan Login Ulang.", Toast.LENGTH_LONG).show()
            // Paksa logout jika token hilang
            navController.navigate("login") { popUpTo(0) }
            return
        }

        // 🔍 DIAGNOSA 2: Pastikan Token diawali "Bearer "
        // Jika di LoginScreen belum pakai "Bearer", kita tambahkan manual disini untuk jaga-jaga
        val finalToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        isLoading = true
        val req = KategoriRequest(namaInfo, keterangan)

        // Debug Log (Cek di Logcat Android kata kunci "TOKEN_CHECK")
        android.util.Log.d("TOKEN_CHECK", "Mengirim Token: $finalToken")

        RetrofitClient.instance.createKategori(finalToken, req).enqueue(object : Callback<Kategori> {
            override fun onResponse(call: Call<Kategori>, response: Response<Kategori>) {
                isLoading = false
                if (response.isSuccessful) {
                    Toast.makeText(context, "Berhasil simpan!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                } else {
                    // Tampilkan pesan error dari server jika ada
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(context, "Gagal ${response.code()}: $errorBody", Toast.LENGTH_LONG).show()
                    android.util.Log.e("API_ERROR", "Error: $errorBody")
                }
            }
            override fun onFailure(call: Call<Kategori>, t: Throwable) {
                isLoading = false
                Toast.makeText(context, "Error Koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Column(modifier = Modifier.padding(24.dp)) {
        Text("Buat Tempat Penarikan Kas", style = MaterialTheme.typography.headlineSmall)
        Text("Masukkan judul dan keterangan untuk Tempat Penarikan Kas Anda.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

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