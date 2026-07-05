package com.vinreader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vinreader.api.VinApiService
import com.vinreader.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val api = VinApiService.create()
    private var searchJob: Job? = null

    /** 拍照识别返回结果 */
    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val vin = result.data?.getStringExtra("vin")
                if (vin != null && vin.length == 17) {
                    binding.etVin.setText(vin)
                    searchVin(vin)
                }
            }
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                scanLauncher.launch(Intent(this, CameraActivity::class.java))
            } else {
                Toast.makeText(this, "需要相机权限才能拍照识别", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupListeners()
    }

    private fun setupListeners() {
        // 相机扫码按钮
        binding.btnScan.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED -> {
                    scanLauncher.launch(Intent(this, CameraActivity::class.java))
                }
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    Toast.makeText(this, "需要相机权限来拍摄车架号", Toast.LENGTH_LONG).show()
                    requestCameraPermission.launch(Manifest.permission.CAMERA)
                }
                else -> {
                    requestCameraPermission.launch(Manifest.permission.CAMERA)
                }
            }
        }

        // 查询按钮
        binding.btnQuery.setOnClickListener {
            val input = binding.etVin.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(this, "请输入或扫描车架号", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!VinValidator.isValidFormat(input)) {
                Toast.makeText(
                    this,
                    "车架号格式无效，应为17位字母数字（不含I/O/Q）",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            searchVin(input.uppercase())
        }

        // 清空按钮
        binding.btnClear.setOnClickListener {
            binding.etVin.setText("")
            binding.etVin.requestFocus()
        }
    }

    private fun searchVin(vin: String) {
        searchJob?.cancel()
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnQuery.isEnabled = false
        binding.btnScan.isEnabled = false

        searchJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.decodeVin(vin)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnQuery.isEnabled = true
                    binding.btnScan.isEnabled = true

                    if (response.Results.isNotEmpty()) {
                        val result = response.Results.first()
                        // NHTSA 返回 ErrorCode="0" 表示成功
                        if (result.ErrorCode == "0" || (result.Make?.isNotEmpty() == true && result.ErrorCode != "4")) {
                            val intent = Intent(this@MainActivity, ResultActivity::class.java)
                            intent.putExtra("vin_result", java.util.HashMap(result.toDisplayMap()))
                            startActivity(intent)
                        } else {
                            val errorMsg = when {
                                result.ErrorCode == "4" -> "VIN 格式无效或校验失败"
                                result.ErrorText?.isNotEmpty() == true -> result.ErrorText
                                else -> "未找到该车架号对应车辆信息"
                            }
                            Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "未查询到车辆信息", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnQuery.isEnabled = true
                    binding.btnScan.isEnabled = true
                    Toast.makeText(
                        this@MainActivity,
                        "网络错误：${e.localizedMessage ?: "连接失败"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        searchJob?.cancel()
        super.onDestroy()
    }
}
