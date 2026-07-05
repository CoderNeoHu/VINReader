package com.vinreader

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vinreader.api.VinApiService
import com.vinreader.databinding.ActivityResultBinding
import kotlinx.coroutines.*

@Suppress("UNCHECKED_CAST")
class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private val api = VinApiService.create()
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar 返回
        binding.toolbar.setNavigationOnClickListener { finish() }
        // 重新查询
        binding.btnNewSearch.setOnClickListener { finish() }

        // 检查是否有从拍照返回的 VIN（直接启动此 Activity 时）
        val scanVin = intent.getStringExtra("vin")
        if (scanVin != null) {
            searchVin(scanVin)
            return
        }

        // 检查是否有缓存的查询结果（从 MainActivity 跳转时）
        val raw = intent.getSerializableExtra("vin_result")
        if (raw is java.util.HashMap<*, *>) {
            val resultMap = raw as java.util.HashMap<String, String>
            displayResults(resultMap)
        } else {
            binding.progressBar.visibility = android.view.View.GONE
            binding.tvError.text = "没有查询数据"
            binding.tvError.visibility = android.view.View.VISIBLE
        }
    }

    private fun searchVin(vin: String) {
        binding.tvTitle.text = "正在查询 VIN: $vin"
        binding.progressBar.visibility = android.view.View.VISIBLE

        searchJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.decodeVin(vin)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    if (response.Results.isNotEmpty()) {
                        val result = response.Results.first()
                        // NHTSA ErrorCode 0=成功, 4=无效VIN, 空=可能部分数据
                        val hasData = result.Make?.isNotEmpty() == true
                        if (result.ErrorCode == "0" || hasData) {
                            displayResults(result.toDisplayMap())
                        } else {
                            binding.tvError.text = "查询失败：${result.ErrorText ?: "未找到信息"}"
                            binding.tvError.visibility = android.view.View.VISIBLE
                        }
                    } else {
                        binding.tvError.text = "未查询到该车架号对应车辆信息"
                        binding.tvError.visibility = android.view.View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.tvError.text = "网络错误：${e.localizedMessage ?: "请检查网络连接"}"
                    binding.tvError.visibility = android.view.View.VISIBLE
                }
            }
        }
    }

    private fun displayResults(map: Map<String, String>) {
        binding.llResults.removeAllViews()
        binding.tvError.visibility = android.view.View.GONE
        binding.llResults.visibility = android.view.View.VISIBLE

        for ((key, value) in map) {
            val inflater = layoutInflater
            val row = inflater.inflate(R.layout.item_result_row, binding.llResults, false)
            val tvKey = row.findViewById<android.widget.TextView>(R.id.tvKey)
            val tvValue = row.findViewById<android.widget.TextView>(R.id.tvValue)
            tvKey.text = key
            tvValue.text = value
            binding.llResults.addView(row)
        }
    }

    override fun onDestroy() {
        searchJob?.cancel()
        super.onDestroy()
    }
}
