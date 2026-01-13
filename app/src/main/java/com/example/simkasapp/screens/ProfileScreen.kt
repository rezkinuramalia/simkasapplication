package com.example.simkasapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.simkasapp.api.RetrofitClient
import com.example.simkasapp.models.*
import com.example.simkasapp.ui.theme.BpsBlue
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.ResponseBody
import com.example.simkasapp.ui.theme.BpsGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, token: String) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("SIMKAS_PREFS", 0)

    // Ensure token format
    val finalToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

    // --- STATES ---
    var isEditing by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    // User Data
    var nama by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nim by remember { mutableStateOf("") }
    var roleName by remember { mutableStateOf("") }

    // Master Data States
    var listKelas by remember { mutableStateOf<List<Kelas>>(emptyList()) }
    var selectedKelas by remember { mutableStateOf<Kelas?>(null) }
    var expandKelas by remember { mutableStateOf(false) }

    // --- LOAD DATA & SYNC ---
    LaunchedEffect(Unit) {
        // 1. Fetch Class List FIRST
        RetrofitClient.instance.getAllKelas(finalToken).enqueue(object : Callback<List<Kelas>> {
            override fun onResponse(call: Call<List<Kelas>>, response: Response<List<Kelas>>) {
                if (response.isSuccessful) {
                    listKelas = response.body() ?: emptyList()

                    // 2. Fetch User Profile AFTER Classes are loaded
                    // This ensures we can match IDs correctly
                    RetrofitClient.instance.getMyProfile(finalToken).enqueue(object : Callback<UserDto> {
                        override fun onResponse(call: Call<UserDto>, res: Response<UserDto>) {
                            res.body()?.let { user ->
                                nama = user.nama
                                nim = user.nim
                                email = user.email
                                phone = user.phone ?: ""
                                roleName = user.roleName

                                // [CRITICAL FIX] Match User's Class ID with Class List
                                if (user.kelasId != null) {
                                    selectedKelas = listKelas.find { it.id == user.kelasId }
                                }
                            }
                        }
                        override fun onFailure(call: Call<UserDto>, t: Throwable) {
                            Toast.makeText(context, "Gagal load profil", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
            override fun onFailure(call: Call<List<Kelas>>, t: Throwable) {
                Toast.makeText(context, "Gagal load kelas", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- SAVE PROFILE FUNCTION ---
    fun saveProfile() {
        val req = UserProfileUpdateRequest(
            nama, email, phone,
            selectedKelas?.id, // Send ID (Int)
            2 // Force Angkatan 65 (ID 2)
        )
        RetrofitClient.instance.updateProfile(finalToken, req).enqueue(object : Callback<UserDto> {
            override fun onResponse(call: Call<UserDto>, response: Response<UserDto>) {
                if(response.isSuccessful) {
                    Toast.makeText(context, "Profil Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                    isEditing = false
                } else {
                    Toast.makeText(context, "Gagal Update: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<UserDto>, t: Throwable) {
                Toast.makeText(context, "Error Koneksi", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- UI LAYOUT ---
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Box(
            modifier = Modifier.size(100.dp).background(BpsBlue, shape = RoundedCornerShape(50.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(60.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(nama, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("$nim | $roleName", color = Color.Gray)
        Spacer(Modifier.height(24.dp))

        // Profile Form Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Edit Button
                if (!isEditing) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit Profil")
                        }
                    }
                }

                if (isEditing) {
                    // EDIT MODE
                    OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No HP") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))

                    // Class Dropdown
                    ExposedDropdownMenuBox(expanded = expandKelas, onExpandedChange = { expandKelas = !expandKelas }) {
                        OutlinedTextField(
                            value = selectedKelas?.nama ?: "Pilih Kelas",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kelas") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandKelas) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandKelas, onDismissRequest = { expandKelas = false }) {
                            listKelas.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.nama) },
                                    onClick = {
                                        selectedKelas = item
                                        expandKelas = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { saveProfile() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BpsBlue)) {
                        Text("SIMPAN")
                    }
                    TextButton(onClick = { isEditing = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("BATAL", color = Color.Gray)
                    }

                } else {
                    // VIEW MODE
                    ProfileItem("Email", email)
                    ProfileItem("No HP", if (phone.isEmpty()) "-" else phone)
                    ProfileItem("Kelas", selectedKelas?.nama ?: "Belum diatur")
                    ProfileItem("Angkatan", "Angkatan 65") // Locked
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Change Password Button
        OutlinedButton(
            onClick = { showPasswordDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BpsGreen)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("GANTI PASSWORD")
        }

        Spacer(Modifier.height(8.dp))

        // Logout Button
        Button(
            onClick = {
                prefs.edit().clear().apply()
                navController.navigate("login") { popUpTo(0) }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("LOGOUT")
        }
    }

    // --- PASSWORD DIALOG ---
    if (showPasswordDialog) {
        ChangePasswordDialog(
            token = finalToken,
            onDismiss = { showPasswordDialog = false }
        )
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Divider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun ChangePasswordDialog(token: String, onDismiss: () -> Unit) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Ganti Password") },
        text = {
            Column {
                OutlinedTextField(value = oldPass, onValueChange = { oldPass = it }, label = { Text("Password Lama") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = newPass, onValueChange = { newPass = it }, label = { Text("Password Baru") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = confirmPass, onValueChange = { confirmPass = it }, label = { Text("Konfirmasi Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            }
        },
        confirmButton = {
            Button(
                enabled = !isLoading,
                onClick = {
                    if (isLoading) return@Button
                    if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                        Toast.makeText(context, "Isi semua kolom", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (newPass != confirmPass) {
                        Toast.makeText(context, "Password baru beda", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    val req = ChangePasswordRequest(oldPass, newPass)

                    // PANGGILAN API DENGAN RESPONSE BODY
                    RetrofitClient.instance.changePassword(token, req).enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                            isLoading = false

                            // KITA CUMA CEK KODE HTTP (200-299 BERARTI SUKSES)
                            if (response.isSuccessful) {
                                Toast.makeText(context, "Password Berhasil Diubah!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                // Ambil pesan error manual
                                val errorMsg = try {
                                    response.errorBody()?.string() ?: "Gagal"
                                } catch (e: Exception) { "Error Server" }
                                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            isLoading = false
                            // Karena kita pakai ResponseBody, parsing error jarang terjadi.
                            // Kalau masuk sini berarti benar-benar koneksi putus.
                            Toast.makeText(context, "Koneksi Bermasalah: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            ) {
                Text(if (isLoading) "Memproses..." else "Simpan")
            }
        },
        dismissButton = {
            if (!isLoading) TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}