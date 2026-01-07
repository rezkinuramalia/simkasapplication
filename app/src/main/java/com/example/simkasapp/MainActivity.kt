package com.example.simkasapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.simkasapp.screens.*
import com.example.simkasapp.ui.theme.SimkasAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimkasAppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()

                    // LOGIKA SESSION
                    val prefs = getSharedPreferences("SIMKAS_PREFS", 0)
                    // Ambil token, jika null ganti dengan string kosong "" agar aman
                    val token = prefs.getString("TOKEN", "") ?: ""

                    // Kalau token ada (tidak kosong) -> Dashboard, Kalau kosong -> Welcome
                    val startDest = if (token.isNotEmpty()) "dashboard" else "welcome"

                    NavHost(navController = navController, startDestination = startDest) {
                        // 1. Halaman Awal
                        composable("welcome") { WelcomeScreen(navController) }

                        // 2. Auth
                        composable("login") { LoginScreen(navController) }
                        composable("register") { RegisterScreen(navController) }

                        // 3. Halaman Utama
                        // [PERBAIKAN DISINI]: Tambahkan parameter token
                        composable("profile") { ProfileScreen(navController, token) }

                        // 4. Fitur Transaksi & Admin
                        composable("upload") { UploadScreen(navController) } // <-- Bayar/Setor
                        composable("create_kategori") { CreateKategoriScreen(navController) } // <-- Buat Wadah Kas

                        // 5. Dashboard Container (Menu Bawah)
                        composable("dashboard") { MainContainerScreen(navController) }

                        // 6. Fitur Wadah & Bayar (Alur Baru)
                        composable("wadah_detail") { WadahDetailScreen(navController) }

                        // Upload Form (Sama dengan upload, tapi nanti dia baca parameter wadah dari navigasi)
                        composable("upload_form") { UploadScreen(navController) }
                    }
                }
            }
        }
    }
}