package com.example.simkasapp.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.simkasapp.R
import com.example.simkasapp.api.RetrofitClient
import com.example.simkasapp.models.LoginRequest
import com.example.simkasapp.models.LoginResponse
import com.example.simkasapp.ui.theme.BpsBlue
import com.example.simkasapp.ui.theme.BpsOrange
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun LoginScreen(navController: NavController) {
    var emailOrNim by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun doLogin() {
        if (emailOrNim.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Isi email/nim dan password!", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        val req = LoginRequest(emailOrNim, password)

        RetrofitClient.instance.login(req).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                isLoading = false
                if (response.isSuccessful) {
                    val body = response.body()
                    val token = "Bearer ${body?.token}"
                    val role = body?.role ?: "ANGGOTA" // Default fallback

                    // Simpan Token & Role
                    val prefs = context.getSharedPreferences("SIMKAS_PREFS", 0)
                    prefs.edit()
                        .putString("TOKEN", token)
                        .putString("ROLE", role)
                        .apply()

                    // Pindah ke Dashboard
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    Toast.makeText(context, "Login Gagal! Cek kredensial.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                isLoading = false
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo (Gunakan logo_stis jika ada)
        Image(
            painter = painterResource(id = R.drawable.logo_stis), // Pastikan file ini ada
            contentDescription = "Logo",
            modifier = Modifier.size(120.dp)
        )
        Spacer(Modifier.height(16.dp))

        Text("SIMKAS", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BpsBlue)
        Text("Masuk ke akun Anda", color = Color.Gray)

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = emailOrNim,
            onValueChange = { emailOrNim = it },
            label = { Text("Email atau NIM") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { doLogin() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BpsBlue),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            Text(if(isLoading) "Loading..." else "MASUK", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { navController.navigate("register") }) {
            Text("Belum punya akun? Daftar", color = BpsOrange)
        }
    }
}