package com.vinreader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vinreader.api.VinApiService
import com.vinreader.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val api = VinApiService.create()
    private var searchJob: Job? = null
    private var lastQueriedVin: String? = null

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
        setupVinInput()
    }

    private fun setupVinInput() {
        // 限制输入只能为 VIN 合法字符
        binding.etVin.filters = arrayOf(
            android.text.InputFilter { source, start, end, _, _, _ ->
                source.substring(start, end).uppercase()
                    .filter { it in VinValidator.VALID_CHARS }
            },
            android.text.InputFilter.LengthFilter(17)
        )
    }

    private fun setupListeners() {
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

        binding.btnQuery.setOnClickListener {
            val input = binding.etVin.text.toString().trim().uppercase()
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
            searchVin(input)
        }

        binding.btnClear.setOnClickListener {
            binding.etVin.setText("")
            binding.etVin.requestFocus()
        }
    }

    private fun searchVin(vin: String) {
        searchJob?.cancel()
        lastQueriedVin = vin
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
                        when {
                            result.ErrorCode == "0" || (result.Make?.isNotEmpty() == true) -> {
                                // 查询成功
                                val intent = Intent(this@MainActivity, ResultActivity::class.java)
                                intent.putExtra("vin_result", java.util.HashMap(result.toDisplayMap()))
                                startActivity(intent)
                            }
                            result.ErrorCode == "4" -> {
                                // VIN 格式/校验错误
                                showVinErrorDialog(vin, "车架号校验失败")
                            }
                            else -> {
                                // 其他 NHTSA 错误
                                val errorText = result.ErrorText ?: result.ErrorCode ?: "未知错误"
                                showVinErrorDialog(vin, errorText)
                            }
                        }
                    } else {
                        showVinErrorDialog(vin, "未查询到该车架号对应车辆信息")
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

    /**
     * VIN 查询失败时弹窗让用户修正
     */
    private fun showVinErrorDialog(originalVin: String, errorMsg: String) {
        // 生成修正建议
        val candidates = VinValidator.smartCorrect(originalVin)
            .filter { it.vin != originalVin }
            .take(3)

        val message = StringBuilder()
        message.append("查询失败：$errorMsg\n\n")
        message.append("原始输入：$originalVin\n")
        message.append("首位 '${originalVin.firstOrNull()}' 代表：${VinValidator.getCountryDescription(originalVin) ?: "未知区域"}\n")
        message.append("中国车 VIN 首位应为 L (代表中国)\n")

        if (candidates.isNotEmpty()) {
            message.append("\n是否尝试以下修正？")
            AlertDialog.Builder(this)
                .setTitle("车架号可能有误")
                .setMessage(message.toString())
                .setNeutralButton("直接重输") { _, _ ->
                    binding.etVin.setText("")
                    binding.etVin.requestFocus()
                }
                .setNegativeButton("关闭") { _, _ -> }
                .setPositiveButton(candidates.first().vin) { _, _ ->
                    binding.etVin.setText(candidates.first().vin)
                    searchVin(candidates.first().vin)
                }
                .show()
        } else {
            message.append("\n请检查车架号是否输入正确")
            AlertDialog.Builder(this)
                .setTitle("车架号可能有误")
                .setMessage(message.toString())
                .setNeutralButton("重新输入") { _, _ ->
                    binding.etVin.setText("")
                    binding.etVin.requestFocus()
                }
                .setNegativeButton("关闭") { _, _ -> }
                .show()
        }
    }

    override fun onDestroy() {
        searchJob?.cancel()
        super.onDestroy()
    }
}
