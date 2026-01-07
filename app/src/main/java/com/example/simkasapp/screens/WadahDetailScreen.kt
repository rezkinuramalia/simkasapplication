package com.example.simkasapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.simkasapp.models.Kategori
import com.example.simkasapp.ui.theme.BpsBlue
import com.example.simkasapp.ui.theme.BpsGreen

@Composable
fun WadahDetailScreen(navController: NavController) {
    // Ambil data wadah yang dikirim dari layar sebelumnya
    val wadah = navController.previousBackStackEntry?.savedStateHandle?.get<Kategori>("wadah")

    if (wadah == null) {
        Text("Data tidak ditemukan")
        return
    }

    Column(modifier = Modifier.padding(24.dp)) {
        Text("Detail Pembayaran", style = MaterialTheme.typography.headlineMedium, color = BpsBlue)
        Spacer(Modifier.height(24.dp))

        Text("Judul Tagihan:", fontWeight = FontWeight.Bold, color = BpsBlue)
        Text(wadah.nama, fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(16.dp))

        Text("Keterangan / Instruksi:", fontWeight = FontWeight.Bold, color = BpsBlue)
        Text(wadah.keterangan ?: "-")

        Spacer(Modifier.height(16.dp))

        Text("Total Terkumpul Saat Ini:", fontWeight = FontWeight.Bold, color = BpsBlue)
        Text("Rp ${wadah.totalTerkumpul}", fontSize = 18.sp, color = BpsGreen)

        Spacer(Modifier.weight(1f)) // Dorong tombol ke bawah

        Button(
            onClick = {
                // Lanjut ke Form Upload dengan membawa ID Wadah ini
                navController.currentBackStackEntry?.savedStateHandle?.set("selectedWadah", wadah)
                navController.navigate("upload_form")
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BpsBlue)
        ) {
            Text("BAYAR SEKARANG")
        }
    }
}