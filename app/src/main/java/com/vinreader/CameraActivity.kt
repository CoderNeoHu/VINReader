package com.vinreader

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
    private var detectedVin: String? = null

    private val photoFile by lazy {
        File(cacheDir, "vin_photo_${System.currentTimeMillis()}.jpg")
    }

    private val imageSavedCallback = object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            try {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                if (bitmap != null) {
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
                    bitmap?.let { runOcr(it) }
                        ?: Toast.makeText(this, "图片读取失败", Toast.LENGTH_SHORT).show()
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
            detectedVin = null
            showCameraPreview()
        }
        binding.btnConfirm.setOnClickListener {
            val vin = detectedVin
            if (vin != null && vin.length == 17) {
                val intent = Intent().putExtra("vin", vin)
                setResult(RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this, "未识别到有效车架号", Toast.LENGTH_SHORT).show()
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
        binding.ivPreview.visibility = android.view.View.GONE
        binding.tvOcrResult.text = "等待识别..."
        binding.tvOcrResult.visibility = android.view.View.GONE
        binding.viewFinder.visibility = android.view.View.VISIBLE
        binding.btnCapture.visibility = android.view.View.VISIBLE
        binding.btnGallery.visibility = android.view.View.VISIBLE
        binding.btnRetake.visibility = android.view.View.GONE
        binding.btnConfirm.visibility = android.view.View.GONE
        detectedVin = null
    }

    private fun showResult(bitmap: Bitmap, vin: String?) {
        binding.viewFinder.visibility = android.view.View.GONE
        binding.btnCapture.visibility = android.view.View.GONE
        binding.btnGallery.visibility = android.view.View.GONE
        binding.ivPreview.visibility = android.view.View.VISIBLE
        binding.ivPreview.setImageBitmap(bitmap)
        binding.tvOcrResult.visibility = android.view.View.VISIBLE

        if (vin != null) {
            detectedVin = vin
            binding.tvOcrResult.text = "识别到车架号：$vin"
            binding.btnRetake.text = "重新拍照"
            binding.btnRetake.visibility = android.view.View.VISIBLE
            binding.btnConfirm.text = "查询此车架号"
            binding.btnConfirm.visibility = android.view.View.VISIBLE
        } else {
            detectedVin = null
            binding.tvOcrResult.text = "未识别到车架号，请重试"
            binding.btnRetake.text = "重新拍照"
            binding.btnRetake.visibility = android.view.View.VISIBLE
            binding.btnConfirm.visibility = android.view.View.GONE
        }
    }

    private fun runOcr(bitmap: Bitmap) {
        binding.tvOcrResult.visibility = android.view.View.VISIBLE
        binding.tvOcrResult.text = "正在识别..."

        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text
                val candidates = VinValidator.extractVinFromText(fullText)

                if (candidates.isNotEmpty()) {
                    val best = candidates.maxByOrNull { c ->
                        if (VinValidator.isValidChecksum(c)) 100 else c.length
                    } ?: candidates.first()
                    showResult(bitmap, best.uppercase())
                } else {
                    showResult(bitmap, null)
                }
                recognizer.close()
            }
            .addOnFailureListener { e ->
                binding.tvOcrResult.text = "识别失败: ${e.localizedMessage ?: "请重试"}"
                showResult(bitmap, null)
                recognizer.close()
            }
    }
}
