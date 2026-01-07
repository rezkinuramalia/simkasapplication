package com.example.simkasapp.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.simkasapp.api.RetrofitClient
import com.example.simkasapp.models.RegisterRequest
import com.example.simkasapp.ui.theme.BpsBlue
import com.example.simkasapp.ui.theme.BpsGreen
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun RegisterScreen(navController: NavController) {
    var nama by remember { mutableStateOf("") }
    var nim by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    fun doRegister() {
        if (password != confirmPassword) {
            Toast.makeText(context, "Konfirmasi Password tidak cocok!", Toast.LENGTH_SHORT).show()
            return
        }
        if (nama.isEmpty() || nim.isEmpty() || email.isEmpty()) {
            Toast.makeText(context, "Lengkapi semua data!", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        // Default Role = 3 (Anggota), Kelas/Angkatan null dulu (dilengkapi di Profil nanti)
        val req = RegisterRequest(nama, nim, email, password)

        RetrofitClient.instance.register(req).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                isLoading = false
                if (response.isSuccessful) {
                    Toast.makeText(context, "Registrasi Berhasil! Silakan Login.", Toast.LENGTH_LONG).show()
                    navController.popBackStack()
                } else {
                    Toast.makeText(context, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                isLoading = false
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Buat Akun Baru", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BpsGreen)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = nim, onValueChange = { nim = it }, label = { Text("NIM") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Konfirmasi Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { doRegister() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BpsGreen),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Memproses..." else "DAFTAR SEKARANG")
        }
    }
}