package com.example.simkasapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.simkasapp.R

@Composable
fun WelcomeScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. LOGO STIS
        Image(
            painter = painterResource(id = R.drawable.logo_stis),
            contentDescription = "Logo STIS",
            modifier = Modifier.size(160.dp)
        )

        Spacer(Modifier.height(32.dp))

        // 2. JUDUL
        Text(
            text = "SIMKAS",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Sistem Manajemen Kas Angkatan",
            fontSize = 16.sp
        )

        Spacer(Modifier.height(64.dp))

        // 3. TOMBOL MASUK
        Button(
            onClick = { navController.navigate("login") },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("LOGIN")
        }

        Spacer(Modifier.height(16.dp))

        // 4. TOMBOL DAFTAR
        OutlinedButton(
            onClick = { navController.navigate("register") },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("REGISTER")
        }
    }
}