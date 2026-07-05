package com.vinreader

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.vinreader.databinding.ActivityCameraBinding
import java.io.File

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var capturedBitmap: Bitmap? = null
    private var ocrRawText: String = ""

    private val photoFile by lazy {
        File(cacheDir, "vin_photo_${System.currentTimeMillis()}.jpg")
    }

    private val imageSavedCallback = object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            try {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                if (bitmap != null) {
                    capturedBitmap = bitmap
                    runOcr(bitmap)
                } else {
                    Toast.makeText(this@CameraActivity, "图片读取失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CameraActivity, "图片处理失败", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onError(exception: ImageCaptureException) {
            Toast.makeText(this@CameraActivity, "拍照失败：${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                try {
                    val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                    if (bitmap != null) {
                        capturedBitmap = bitmap
                        runOcr(bitmap)
                    } else {
                        Toast.makeText(this, "图片读取失败", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "图片读取失败：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupListeners()
        startCamera()
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnCapture.setOnClickListener { takePhoto() }
        binding.btnGallery.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnRetake.setOnClickListener {
            confirmedVin = null
            capturedBitmap = null
            ocrRawText = ""
            showCameraPreview()
        }
        binding.btnConfirm.setOnClickListener {
            val vin = binding.etVinEdit.text.toString().trim().uppercase()
            if (vin.length == 17 && VinValidator.isValidFormat(vin)) {
                confirmedVin = vin
                val intent = Intent().putExtra("vin", vin)
                setResult(RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this, "请输入有效的17位车架号", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return
        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            provider.unbindAll()
            provider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        } catch (e: Exception) {
            Toast.makeText(this, "相机启动失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun takePhoto() {
        val capture = imageCapture ?: run {
            Toast.makeText(this, "相机未就绪", Toast.LENGTH_SHORT).show()
            return
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        capture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), imageSavedCallback)
    }

    private fun showCameraPreview() {
        binding.ivPreview.visibility = View.GONE
        binding.tvOcrResult.visibility = View.GONE
        binding.tilVinEdit.visibility = View.GONE
        binding.etVinEdit.visibility = View.GONE
        binding.tvEditHint.visibility = View.GONE
        binding.tvSuggestions.visibility = View.GONE
        binding.viewFinder.visibility = View.VISIBLE
        binding.btnCapture.visibility = View.VISIBLE
        binding.btnGallery.visibility = View.VISIBLE
        binding.btnRetake.visibility = View.GONE
        binding.btnConfirm.visibility = View.GONE
        binding.btnSwitchVin.visibility = View.GONE
    }

    private fun showResults(bitmap: Bitmap) {
        binding.viewFinder.visibility = View.GONE
        binding.btnCapture.visibility = View.GONE
        binding.btnGallery.visibility = View.GONE
        binding.ivPreview.visibility = View.VISIBLE
        binding.ivPreview.setImageBitmap(bitmap)
        binding.tilVinEdit.visibility = View.VISIBLE
        binding.etVinEdit.visibility = View.VISIBLE
        binding.tvEditHint.visibility = View.VISIBLE
        binding.btnRetake.text = "重新拍照"
        binding.btnRetake.visibility = View.VISIBLE
        binding.btnConfirm.visibility = View.VISIBLE

        // Use smart correction
        val candidates = VinValidator.smartCorrect(ocrRawText)

        if (candidates.isNotEmpty()) {
            val best = candidates.first()
            binding.etVinEdit.setText(best.vin)
            binding.tvOcrResult.text = "识别结果 (可编辑修正)"
            binding.tvOcrResult.visibility = View.VISIBLE

            // 显示其他候选
            if (candidates.size > 1) {
                binding.tvSuggestions.visibility = View.VISIBLE
                val sb = StringBuilder("其他候选：\n")
                candidates.drop(1).forEachIndexed { i, c ->
                    sb.append("${i + 1}. ${c.vin}")
                    if (c.checksumValid) sb.append(" ✓")
                    if (c.countryCodeKnown) sb.append(" [${VinValidator.getCountryDescription(c.vin) ?: ""}]")
                    sb.append("\n")
                }
                binding.tvSuggestions.text = sb.toString()
                binding.btnSwitchVin.visibility = View.VISIBLE
            } else {
                binding.tvSuggestions.visibility = View.GONE
                binding.btnSwitchVin.visibility = View.GONE
            }

            // 校验位和国家提示
            val hints = mutableListOf<String>()
            if (best.checksumValid) hints.add("校验通过 ✓")
            else hints.add("校验位不符 ⚠")
            if (best.countryCodeKnown) {
                val country = VinValidator.getCountryDescription(best.vin) ?: ""
                hints.add("VIN 首位 ${best.vin[0]} = $country")
            }

            binding.tvEditHint.text = "提示: ${hints.joinToString(" | ")}\n" +
                    "中国车 VIN 首位应为 L，如识别为 7 请手动改为 L"

            // 点击切换候选
            switchCandidates(candidates)
        } else {
            // 无候选，显示原始文本
            val raw = ocrRawText.take(200)
            binding.etVinEdit.setText("")
            binding.tvOcrResult.text = "未识别到完整车架号"
            binding.tvOcrResult.visibility = View.VISIBLE
            binding.tvEditHint.text = "下方显示了识别到的文字，请手动找到 VIN 输入\n识别文字：$raw"
            binding.tvSuggestions.visibility = View.GONE
            binding.btnSwitchVin.visibility = View.GONE
        }
    }

    /** 设置最大长度过滤器 */
    private fun setupVinInputFilters() {
        binding.etVinEdit.filters = arrayOf(
            InputFilter { source, start, end, _, _, _ ->
                val filtered = source.substring(start, end)
                    .uppercase()
                    .filter { it in VinValidator.VALID_CHARS || it in setOf('O', 'I', 'Q') }
                if (filtered != source.substring(start, end)) filtered else null
            },
            InputFilter.LengthFilter(17)
        )
    }

    private var candidateList = listOf<VinValidator.CandidateVin>()
    private var currentCandidateIndex = 0

    private fun switchCandidates(candidates: List<VinValidator.CandidateVin>) {
        candidateList = candidates
        currentCandidateIndex = 0

        binding.btnSwitchVin.setOnClickListener {
            if (candidateList.size <= 1) return@setOnClickListener
            currentCandidateIndex = (currentCandidateIndex + 1) % candidateList.size
            val c = candidateList[currentCandidateIndex]
            binding.etVinEdit.setText(c.vin)
            val country = VinValidator.getCountryDescription(c.vin) ?: "未知"
            Toast.makeText(this, "候选 ${currentCandidateIndex + 1}/${candidateList.size}: ${c.vin} [$country]", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runOcr(bitmap: Bitmap) {
        binding.tvOcrResult.visibility = View.VISIBLE
        binding.tvOcrResult.text = "正在识别..."

        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                ocrRawText = visionText.text
                showResults(bitmap)
                recognizer.close()
            }
            .addOnFailureListener { e ->
                ocrRawText = ""
                Toast.makeText(this, "识别失败: ${e.localizedMessage ?: "请重试"}", Toast.LENGTH_SHORT).show()
                showCameraPreview()
                recognizer.close()
            }
    }
}
