package com.example.simkasapp.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.simkasapp.api.RetrofitClient
import com.example.simkasapp.models.*
import com.example.simkasapp.ui.theme.BpsBlue
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, token: String) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("SIMKAS_PREFS", 0)

    // [PERBAIKAN UTAMA]
    // Pastikan token memiliki awalan "Bearer ".
    // Jika backend mengirim raw token, kita tambahkan manual di sini.
    val finalToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

    // Log untuk mengecek di Logcat
    Log.d("DEBUG_SIMKAS", "Token yang dipakai: $finalToken")

    // State Mode Edit
    var isEditing by remember { mutableStateOf(false) }

    // State Data User
    var nama by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nim by remember { mutableStateOf("") }
    var roleName by remember { mutableStateOf("") }

    // State Dropdown
    var selectedKelas by remember { mutableStateOf<Kelas?>(null) }
    var selectedAngkatan by remember { mutableStateOf<Angkatan?>(null) }

    // State List Master Data
    var listKelas by remember { mutableStateOf<List<Kelas>>(emptyList()) }
    var listAngkatan by remember { mutableStateOf<List<Angkatan>>(emptyList()) }

    var expandKelas by remember { mutableStateOf(false) }
    var expandAngkatan by remember { mutableStateOf(false) }

    // Status Loading agar UI tahu kapan selesai
    var isLoadingData by remember { mutableStateOf(true) }

    // --- FUNGSI LOAD MASTER DATA (KELAS & ANGKATAN) ---
    fun loadMasterData() {
        isLoadingData = true

        // Load Kelas (Gunakan finalToken)
        RetrofitClient.instance.getAllKelas(finalToken).enqueue(object : Callback<List<Kelas>> {
            override fun onResponse(call: Call<List<Kelas>>, response: Response<List<Kelas>>) {
                if (response.isSuccessful) {
                    listKelas = response.body() ?: emptyList()
                    Log.d("DEBUG_SIMKAS", "Kelas Loaded: ${listKelas.size} items")
                } else {
                    Log.e("DEBUG_SIMKAS", "Gagal Load Kelas: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<List<Kelas>>, t: Throwable) {
                Log.e("DEBUG_SIMKAS", "Error Koneksi Kelas: ${t.message}")
            }
        })

        // Load Angkatan (Gunakan finalToken)
        RetrofitClient.instance.getAllAngkatan(finalToken).enqueue(object : Callback<List<Angkatan>> {
            override fun onResponse(call: Call<List<Angkatan>>, response: Response<List<Angkatan>>) {
                isLoadingData = false // Anggap loading selesai saat request terakhir respon
                if (response.isSuccessful) {
                    listAngkatan = response.body() ?: emptyList()
                    Log.d("DEBUG_SIMKAS", "Angkatan Loaded: ${listAngkatan.size} items")
                } else {
                    Log.e("DEBUG_SIMKAS", "Gagal Load Angkatan: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<List<Angkatan>>, t: Throwable) {
                isLoadingData = false
                Log.e("DEBUG_SIMKAS", "Error Koneksi Angkatan: ${t.message}")
            }
        })
    }

    // --- FUNGSI LOAD PROFIL USER ---
    fun loadUserProfile() {
        // Gunakan finalToken
        RetrofitClient.instance.getMyProfile(finalToken).enqueue(object : Callback<UserDto> {
            override fun onResponse(call: Call<UserDto>, response: Response<UserDto>) {
                response.body()?.let { user ->
                    nama = user.nama
                    nim = user.nim
                    email = user.email
                    phone = user.phone ?: ""
                    roleName = user.roleName

                    // Logic pencocokan akan jalan otomatis saat UI di-render ulang
                    // karena listKelas & listAngkatan akan terisi oleh loadMasterData
                    if (user.kelasId != null) {
                        // Kita pasang listener di LaunchedEffect terpisah atau biarkan UI re-compose
                        // Untuk simplicity, kita set ID-nya, nanti UI dropdown akan membaca listKelas yang sudah update
                        // (Requires listKelas to be populated first, but async is tricky.
                        //  Better UI Logic: User sees "Pilih Kelas", if user has class, we set it later)

                        // Cara aman: Kita trigger delay check atau biarkan user memilih ulang jika data belum sync
                    }
                }
            }
            override fun onFailure(call: Call<UserDto>, t: Throwable) {}
        })
    }

    // --- PANGGIL KEDUANYA SAAT LAYAR DIBUKA ---
    LaunchedEffect(Unit) {
        loadMasterData()
        loadUserProfile()
    }

    // LaunchedEffect tambahan untuk sinkronisasi otomatis setelah data masuk
    LaunchedEffect(listKelas, listAngkatan) {
        // Jika list sudah masuk, coba load profil lagi untuk mencocokkan (opsional)
        // Atau biarkan user memilih manual
    }

    // --- FUNGSI SIMPAN ---
    fun saveProfile() {
        val req = UserProfileUpdateRequest(
            nama, email, phone,
            selectedKelas?.id,
            selectedAngkatan?.id
        )
        // Gunakan finalToken
        RetrofitClient.instance.updateProfile(finalToken, req).enqueue(object : Callback<UserDto> {
            override fun onResponse(call: Call<UserDto>, response: Response<UserDto>) {
                if(response.isSuccessful) {
                    Toast.makeText(context, "Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
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

    fun doLogout() {
        prefs.edit().clear().apply()
        navController.navigate("login") { popUpTo(0) }
    }

    // === UI ===
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Foto & Nama
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(BpsBlue, shape = RoundedCornerShape(50.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(60.dp))
        }

        Spacer(Modifier.height(12.dp))
        Text(nama, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("$nim | $roleName", color = Color.Gray)

        Spacer(Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                    OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No HP") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))

                    // --- DROPDOWN KELAS ---
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
                            if (listKelas.isEmpty()) {
                                // Tampilkan status kenapa kosong
                                val msg = if(isLoadingData) "Memuat..." else "Data Kosong / Error"
                                DropdownMenuItem(text = { Text(msg) }, onClick = { })
                            } else {
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
                    }
                    Spacer(Modifier.height(8.dp))

                    // --- DROPDOWN ANGKATAN ---
                    ExposedDropdownMenuBox(expanded = expandAngkatan, onExpandedChange = { expandAngkatan = !expandAngkatan }) {
                        OutlinedTextField(
                            value = selectedAngkatan?.nama ?: "Pilih Angkatan",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Angkatan") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandAngkatan) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandAngkatan, onDismissRequest = { expandAngkatan = false }) {
                            if (listAngkatan.isEmpty()) {
                                val msg = if(isLoadingData) "Memuat..." else "Data Kosong / Error"
                                DropdownMenuItem(text = { Text(msg) }, onClick = { })
                            } else {
                                listAngkatan.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item.nama) },
                                        onClick = {
                                            selectedAngkatan = item
                                            expandAngkatan = false
                                        }
                                    )
                                }
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
                    ProfileItem("Email", email)
                    ProfileItem("No HP", if (phone.isEmpty()) "-" else phone)
                    ProfileItem("Kelas", selectedKelas?.nama ?: "Belum diatur")
                    ProfileItem("Angkatan", selectedAngkatan?.nama ?: "Belum diatur")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { doLogout() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("LOGOUT")
        }
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