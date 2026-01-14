package com.example.simkasapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.simkasapp.models.Kategori
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
                    val token = prefs.getString("TOKEN", "") ?: ""

                    // Kalau token ada -> Dashboard, Kalau kosong -> Welcome
                    val startDest = if (token.isNotEmpty()) "dashboard" else "welcome"

                    NavHost(navController = navController, startDestination = startDest) {
                        // 1. Halaman Awal & Auth
                        composable("welcome") { WelcomeScreen(navController) }
                        composable("login") { LoginScreen(navController) }
                        composable("register") { RegisterScreen(navController) }

                        // 2. Dashboard Container (Menu Bawah: Beranda, Bayar, Riwayat, Profil)
                        composable("dashboard") { MainContainerScreen(navController) }

                        // 3. Profil
                        composable("profile") { ProfileScreen(navController, token) }

                        // 4. Fitur Create (Buat Wadah)
                        composable("create_kategori") { CreateKategoriScreen(navController) }

                        // 5. Fitur Upload / Bayar (Form)
                        composable("upload") { UploadScreen(navController) }
                        composable("upload_form") { UploadScreen(navController) }

                        // 6. Detail Wadah (Untuk User MEMBER membayar)
                        composable("wadah_detail") { WadahDetailScreen(navController) }

                        // ================================================================
                        // 7. [TAMBAHAN PENTING] Validasi Wadah (Untuk ADMIN/BENDAHARA)
                        //    Ini yang sebelumnya hilang dan bikin crash!
                        // ================================================================
                        composable("wadah_validasi_screen") {
                            // Ambil data 'wadah' yang dikirim dari DashboardScreen
                            val wadah = navController.previousBackStackEntry?.savedStateHandle?.get<Kategori>("wadah")

                            if (wadah != null) {
                                WadahValidasiScreen(navController, token, wadah)
                            } else {
                                // Jaga-jaga jika data null, kembalikan ke Dashboard
                                MainContainerScreen(navController)
                            }
                        }
                    }
                }
            }
        }
    }
}