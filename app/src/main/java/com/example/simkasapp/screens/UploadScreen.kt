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
import com.example.simkasapp.R
import com.example.simkasapp.api.RetrofitClient
import com.example.simkasapp.models.Kategori
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("SIMKAS_PREFS", 0)
    val token = prefs.getString("TOKEN", "") ?: ""

    // Ambil ID User
    val userId = prefs.getInt("ID_USER", 0)

    // === [UPDATE BARU] AMBIL DATA DARI NAVIGASI ===
    // Cek apakah ada data wadah yang dikirim dari WadahDetailScreen?
    val passedWadah = remember {
        navController.previousBackStackEntry?.savedStateHandle?.get<Kategori>("selectedWadah")
    }

    // State Form
    var nominal by remember { mutableStateOf("") }
    var keterangan by remember { mutableStateOf("") }

    // Jika passedWadah ada, jadikan default selectedKategori. Jika tidak, null.
    var selectedKategori by remember { mutableStateOf<Kategori?>(passedWadah) }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var listKategori by remember { mutableStateOf<List<Kategori>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) } // Dropdown state
    var isLoading by remember { mutableStateOf(false) }

    // Ambil Kategori dari Backend (Untuk mengisi dropdown jika user mau ganti)
    LaunchedEffect(Unit) {
        RetrofitClient.instance.getAllKategori(token).enqueue(object : Callback<List<Kategori>> {
            override fun onResponse(call: Call<List<Kategori>>, response: Response<List<Kategori>>) {
                if(response.isSuccessful) {
                    listKategori = response.body() ?: emptyList()

                    // Logic tambahan: Jika ada passedWadah, pastikan dia terpilih di list
                    if (passedWadah != null) {
                        // Tidak perlu ubah selectedKategori lagi karena sudah di-init di atas
                    }
                }
            }
            override fun onFailure(call: Call<List<Kategori>>, t: Throwable) {}
        })
    }

    // File Picker
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    fun doUpload() {
        if (nominal.isEmpty() || selectedKategori == null || imageUri == null) {
            Toast.makeText(context, "Mohon lengkapi nominal, tempat bayar, dan bukti!", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true

        // 1. Proses File Gambar
        val file = File(context.cacheDir, "bukti_bayar.jpg")
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

        val requestFile = okhttp3.RequestBody.create("image/*".toMediaTypeOrNull(), file)
        val bodyImage = MultipartBody.Part.createFormData("file", file.name, requestFile)

        // 2. Siapkan Data JSON
        val jsonString = """
            {
                "idUser": $userId, 
                "idKategori": ${selectedKategori!!.id},
                "nominal": ${nominal},
                "keterangan": "$keterangan",
                "jenisTransaksi": "PEMASUKAN",
                "bulanKas": 1, 
                "tahunKas": 2025
            }
        """.trimIndent()

        val bodyData = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

        // 3. Panggil API
        RetrofitClient.instance.createTransaksi(token, bodyData, bodyImage).enqueue(object : Callback<TransaksiResponse> {
            override fun onResponse(call: Call<TransaksiResponse>, response: Response<TransaksiResponse>) {
                isLoading = false
                if (response.isSuccessful) {
                    Toast.makeText(context, "Berhasil Upload Bukti!", Toast.LENGTH_LONG).show()
                    navController.popBackStack() // Kembali ke layar sebelumnya
                    // Hapus data savedState agar tidak nyangkut jika buka form lagi
                    navController.currentBackStackEntry?.savedStateHandle?.remove<Kategori>("selectedWadah")
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal: ${response.code()}"
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<TransaksiResponse>, t: Throwable) {
                isLoading = false
                Toast.makeText(context, "Koneksi Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Form Pembayaran", style = MaterialTheme.typography.headlineMedium, color = BpsBlue)
        Spacer(Modifier.height(16.dp))

        // DROPDOWN TEMPAT BAYAR
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedKategori?.nama ?: "Pilih Tempat Bayar",
                onValueChange = {},
                readOnly = true,
                label = { Text("Bayar Ke (Wadah Kas)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listKategori.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.nama) },
                        onClick = {
                            selectedKategori = item
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = nominal,
            onValueChange = { nominal = it },
            label = { Text("Nominal (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = keterangan,
            onValueChange = { keterangan = it },
            label = { Text("Keterangan Tambahan") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text("Bukti Pembayaran", fontWeight = FontWeight.Bold)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                Image(painter = rememberAsyncImagePainter(imageUri), contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(painterResource(android.R.drawable.ic_menu_camera), contentDescription = null, tint = Color.Gray)
                    Text("Klik untuk pilih gambar", color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { doUpload() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BpsBlue),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Mengirim..." else "KIRIM BUKTI")
        }
    }
}