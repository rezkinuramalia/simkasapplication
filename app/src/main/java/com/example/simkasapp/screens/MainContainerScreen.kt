package com.example.simkasapp.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.simkasapp.ui.theme.PastelBlue
import com.example.simkasapp.ui.theme.PastelOrange

// Define Menu Items
sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Kelola", Icons.Default.Home)
    object Pay : BottomNavItem("pay", "Setor", Icons.Default.ShoppingCart)
    object History : BottomNavItem("history", "Riwayat", Icons.Default.List)
    object Profile : BottomNavItem("profile", "Profil", Icons.Default.Person)
}

@Composable
fun MainContainerScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("SIMKAS_PREFS", 0)
    val token = prefs.getString("TOKEN", "") ?: ""
    val role = prefs.getString("ROLE", "ANGGOTA") ?: "ANGGOTA"

    var currentScreen by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }

    // === LOGIKA MENU DINAMIS ===
    val items = remember(role) {
        if (role == "ADMIN_ANGKATAN") {
            // Admin Angkatan: Hapus Menu Setor & Riwayat
            // Hanya bisa Mengelola (Home) dan lihat Profil
            listOf(
                BottomNavItem.Home,
                BottomNavItem.Profile
            )
        } else {
            // Role Lain (Anggota/Bendahara): Tampilkan Semua
            listOf(
                BottomNavItem.Home,
                BottomNavItem.Pay,
                BottomNavItem.History,
                BottomNavItem.Profile
            )
        }
    }

    Scaffold(
        // === BOTTOM NAVIGATION BAR ===
        bottomBar = {
            NavigationBar(containerColor = PastelBlue) { // Pakai warna PastelBlue
                items.forEach { item ->
                    val isSelected = currentScreen == item

                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = isSelected,
                        onClick = { currentScreen = item },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PastelBlue, // Ikon terpilih biru pastel
                            selectedTextColor = PastelOrange, // Teks terpilih orange pastel
                            indicatorColor = Color.White, // Lingkaran background putih
                            unselectedIconColor = Color.White.copy(alpha = 0.7f),
                            unselectedTextColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                BottomNavItem.Home -> DashboardScreenContent(token, role, navController)
                BottomNavItem.Pay -> WadahListScreen(navController, token)
                BottomNavItem.History -> HistoryScreen(navController, token)
                BottomNavItem.Profile -> ProfileScreen(navController, token)
                else -> {} // Fallback aman
            }
        }
    }
}