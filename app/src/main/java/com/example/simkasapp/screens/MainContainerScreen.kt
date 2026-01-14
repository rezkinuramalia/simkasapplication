package com.example.simkasapp.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.simkasapp.ui.theme.BpsBlue
import com.example.simkasapp.ui.theme.BpsOrange

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

    Scaffold(
        // === TOMBOL TAMBAH (+) DI TENGAH ===
        // Hanya muncul jika user BUKAN Anggota biasa (Admin / Bendahara)
        floatingActionButton = {
            if (role != "ANGGOTA") {
                FloatingActionButton(
                    onClick = { navController.navigate("create_kategori") }, // Ke layar buat wadah
                    containerColor = BpsOrange,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Buat Wadah")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center, // Posisi di tengah

        // === BOTTOM NAVIGATION BAR ===
        bottomBar = {
            NavigationBar(containerColor = BpsBlue) {
                val items = listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Pay,
                    BottomNavItem.History,
                    BottomNavItem.Profile
                )

                items.forEach { item ->
                    // Jika role bukan anggota, beri jarak di tengah untuk tombol (+)
                    // Logika sederhana: Pay di kiri, History di kanan
                    val isSelected = currentScreen == item

                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = isSelected,
                        onClick = { currentScreen = item },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BpsBlue,
                            selectedTextColor = BpsOrange,
                            indicatorColor = BpsOrange,
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
                BottomNavItem.History -> Text("Halaman Riwayat (Segera Hadir)") // Nanti diganti HistoryScreen
                BottomNavItem.Profile -> ProfileScreen(navController, token)
            }
        }
    }
}