package com.example.simkasapp.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.simkasapp.api.RetrofitClient
import com.example.simkasapp.models.Angkatan
import com.example.simkasapp.models.Kategori
import com.example.simkasapp.models.Kelas
import com.example.simkasapp.models.TransaksiResponse
import com.example.simkasapp.ui.theme.BpsBlue
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("SIMKAS_PREFS", 0)
    val token = prefs.getString("TOKEN", "") ?: ""
    // ID User diambil dari prefs
    val userId = prefs.getInt("ID_USER", 0)

    // === STATE INPUT ===
    var nama by remember { mutableStateOf("") }
    var nim by remember { mutableStateOf("") }
    var nominal by remember { mutableStateOf("") }

    // Dropdown Data
    var selectedKelas by remember { mutableStateOf<Kelas?>(null) }
    var selectedAngkatan by remember { mutableStateOf<Angkatan?>(null) }

    // Wadah/Kategori (Bisa dari navigasi atau pilih manual)
    val passedWadah = remember {
        navController.previousBackStackEntry?.savedStateHandle?.get<Kategori>("selectedWadah")
    }
    var selectedKategori by remember { mutableStateOf<Kategori?>(passedWadah) }

    // List untuk Dropdown
    var listKelas by remember { mutableStateOf<List<Kelas>>(emptyList()) }
    var listAngkatan by remember { mutableStateOf<List<Angkatan>>(emptyList()) }
    var listKategori by remember { mutableStateOf<List<Kategori>>(emptyList()) }

    // Expand State
    var expandKelas by remember { mutableStateOf(false) }
    var expandAngkatan by remember { mutableStateOf(false) }
    var expandKategori by remember { mutableStateOf(false) }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // === LOAD DATA DARI SERVER ===
    LaunchedEffect(Unit) {
        val finalToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        // 1. Load Kategori
        RetrofitClient.instance.getAllKategori(finalToken).enqueue(object : Callback<List<Kategori>> {
            override fun onResponse(call: Call<List<Kategori>>, response: Response<List<Kategori>>) {
                if(response.isSuccessful) listKategori = response.body() ?: emptyList()
            }
            override fun onFailure(call: Call<List<Kategori>>, t: Throwable) {}
        })

        // 2. Load Kelas
        RetrofitClient.instance.getAllKelas(finalToken).enqueue(object : Callback<List<Kelas>> {
            override fun onResponse(call: Call<List<Kelas>>, response: Response<List<Kelas>>) {
                if(response.isSuccessful) listKelas = response.body() ?: emptyList()
            }
            override fun onFailure(call: Call<List<Kelas>>, t: Throwable) {}
        })

        // 3. Load Angkatan
        RetrofitClient.instance.getAllAngkatan(finalToken).enqueue(object : Callback<List<Angkatan>> {
            override fun onResponse(call: Call<List<Angkatan>>, response: Response<List<Angkatan>>) {
                if(response.isSuccessful) listAngkatan = response.body() ?: emptyList()
            }
            override fun onFailure(call: Call<List<Angkatan>>, t: Throwable) {}
        })
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    // === FUNGSI KIRIM ===
    fun doSubmitForm() {
        if (nama.isEmpty() || nim.isEmpty() || selectedKelas == null ||
            selectedAngkatan == null || selectedKategori == null || nominal.isEmpty() || imageUri == null) {
            Toast.makeText(context, "Mohon lengkapi semua data!", Toast.LENGTH_LONG).show()
            return
        }

        isLoading = true

        // 1. Proses Gambar dari URI ke File
        val file = File(context.cacheDir, "bukti_bayar_${System.currentTimeMillis()}.jpg")
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri!!)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
        } catch (e: Exception) {
            isLoading = false
            Toast.makeText(context, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Konversi File ke RequestBody (Langsung di sini agar tidak error reference)
        val requestFile = file.readBytes().toRequestBody("image/*".toMediaTypeOrNull())
        val bodyImage = MultipartBody.Part.createFormData("file", file.name, requestFile)

        // 3. Gabungkan Nama & NIM ke Keterangan
        val keteranganLengkap = "Pembayaran dari: $nama ($nim)"

        val calendar = Calendar.getInstance()
        val bulanIni = calendar.get(Calendar.MONTH) + 1
        val tahunIni = calendar.get(Calendar.YEAR)

        // 4. JSON Payload
        val jsonString = """
            {
                "idUser": $userId, 
                "idKategori": ${selectedKategori!!.id},
                "idKelas": ${selectedKelas!!.id},
                "idAngkatan": ${selectedAngkatan!!.id},
                "nominal": ${nominal},
                "keterangan": "$keteranganLengkap",
                "jenisTransaksi": "PEMASUKAN",
                "bulanKas": $bulanIni, 
                "tahunKas": $tahunIni
            }
        """.trimIndent()

        val bodyData = jsonString.toRequestBody("application/json".toMediaTypeOrNull())
        val finalToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        // 5. Kirim ke API
        RetrofitClient.instance.createTransaksi(finalToken, bodyData, bodyImage).enqueue(object : Callback<TransaksiResponse> {
            override fun onResponse(call: Call<TransaksiResponse>, response: Response<TransaksiResponse>) {
                isLoading = false
                if (response.isSuccessful) {
                    Toast.makeText(context, "Form Terkirim! Menunggu Validasi.", Toast.LENGTH_LONG).show()

                    // Trigger agar halaman Riwayat merefresh datanya
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh_history", true)

                    // Kembali ke layar sebelumnya
                    navController.popBackStack()
                } else {
                    val errorMsg = "Gagal: ${response.code()} ${response.message()}"
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<TransaksiResponse>, t: Throwable) {
                isLoading = false
                Toast.makeText(context, "Error Koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // === UI ===
    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Form Pembayaran Kas", style = MaterialTheme.typography.headlineMedium, color = BpsBlue)
        Text("Isi data diri & bukti bayar", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(value = nim, onValueChange = { nim = it }, label = { Text("NIM") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        // Dropdown Kelas
        ExposedDropdownMenuBox(expanded = expandKelas, onExpandedChange = { expandKelas = it }) {
            OutlinedTextField(
                value = selectedKelas?.nama ?: "Pilih Kelas", onValueChange = {}, readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandKelas) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expandKelas, onDismissRequest = { expandKelas = false }) {
                listKelas.forEach { item ->
                    DropdownMenuItem(text = { Text(item.nama) }, onClick = { selectedKelas = item; expandKelas = false })
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Dropdown Angkatan
        ExposedDropdownMenuBox(expanded = expandAngkatan, onExpandedChange = { expandAngkatan = it }) {
            OutlinedTextField(
                value = selectedAngkatan?.nama ?: "Pilih Angkatan", onValueChange = {}, readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandAngkatan) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expandAngkatan, onDismissRequest = { expandAngkatan = false }) {
                listAngkatan.forEach { item ->
                    DropdownMenuItem(text = { Text(item.nama) }, onClick = { selectedAngkatan = item; expandAngkatan = false })
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Dropdown Wadah
        ExposedDropdownMenuBox(expanded = expandKategori, onExpandedChange = { expandKategori = it }) {
            OutlinedTextField(
                value = selectedKategori?.nama ?: "Bayar Ke (Wadah)", onValueChange = {}, readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandKategori) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expandKategori, onDismissRequest = { expandKategori = false }) {
                listKategori.forEach { item ->
                    DropdownMenuItem(text = { Text(item.nama) }, onClick = { selectedKategori = item; expandKategori = false })
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(value = nominal, onValueChange = { nominal = it }, label = { Text("Nominal (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        Text("Bukti Pembayaran", fontWeight = FontWeight.Bold, color = BpsBlue)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable { launcher.launch("image/*") }
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                Image(painter = rememberAsyncImagePainter(imageUri), contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0)), modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(painterResource(android.R.drawable.ic_menu_camera), contentDescription = null, tint = Color.Gray)
                        Text("Klik upload bukti", color = Color.Gray)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { doSubmitForm() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BpsBlue),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Mengirim..." else "SUBMIT FORM", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(30.dp))
    }
}