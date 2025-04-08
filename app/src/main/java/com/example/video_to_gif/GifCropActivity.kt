package com.example.video_to_gif

import android.content.Intent
import android.graphics.Matrix
import android.graphics.Rect
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.io.FileOutputStream

class GifCropActivity : AppCompatActivity() {

    private lateinit var gifView: ImageView
    private lateinit var cropView: CropView
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var returnButton: Button

    private val matrix = Matrix()
    private var scaleFactor = 1f
    private var inputGifUri: Uri? = null
    private var gifWidth = 0
    private var gifHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gif_crop)

        initViews()

        inputGifUri = Uri.parse(intent.getStringExtra("GIF_URI"))

        if (inputGifUri != null) {
            val inputStream = contentResolver.openInputStream(inputGifUri!!)
            val tempFile = File.createTempFile("temp_gif", ".gif", cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                inputStream?.copyTo(outputStream)
            }
            val bitmap = android.graphics.BitmapFactory.decodeFile(tempFile.absolutePath)
            gifWidth = bitmap.width
            gifHeight = bitmap.height
            gifView.setImageBitmap(bitmap)

            // 计算缩放比例并动态调整 CropView 的大小
            gifView.viewTreeObserver.addOnGlobalLayoutListener {
                // 仅计算缩放比例
                val scaleFactor = Math.min(
                    gifView.width.toFloat() / gifWidth,
                    gifView.height.toFloat() / gifHeight
                )

                matrix.setScale(scaleFactor, scaleFactor)
                gifView.imageMatrix = matrix

                // 设置 CropView 的参考尺寸为缩放后的实际显示尺寸
                cropView.setImageSize(
                    gifWidth * scaleFactor,
                    gifHeight * scaleFactor
                )
            }

            setupImageMatrixListener()
        } else {
            showError("无效的文件路径")
        }

        setupButton()
    }

    private fun initViews() {
        gifView = findViewById(R.id.gif_view)
        cropView = findViewById(R.id.crop_view)
        saveButton = findViewById(R.id.save_button)
        cancelButton = findViewById(R.id.cancel_button)
        returnButton = findViewById(R.id.return_button)
    }

    private fun setupButton() {
        saveButton.setOnClickListener {
            saveCroppedGif()
        }

        // 取消返回上一页
        cancelButton.setOnClickListener {
            finish()
        }

        // 返回按钮：直接返回主页
        returnButton.setOnClickListener {
            val intnet = Intent(this,MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intnet)
        }
    }

    private fun setupImageMatrixListener() {
        gifView.viewTreeObserver.addOnGlobalLayoutListener {
            val matrixValues = FloatArray(9)
            gifView.imageMatrix.getValues(matrixValues)

            scaleFactor = matrixValues[Matrix.MSCALE_X]
            val translateX = matrixValues[Matrix.MTRANS_X]
            val translateY = matrixValues[Matrix.MTRANS_Y]

            matrix.reset()
            matrix.postScale(1 / scaleFactor, 1 / scaleFactor)
            matrix.postTranslate(-translateX / scaleFactor, -translateY / scaleFactor)
        }
    }

    private fun saveCroppedGif() {
        try {
            val cropRect = getAdjustedCropRect()
            val outputFile = createOutputFile()
            encodeGif(cropRect, outputFile)
            showSuccess(outputFile)

            // 跳转到下一个 Activity，并传递文件路径
            val intent = Intent(this, GifPlayActivity::class.java).apply {
                putExtra("GIF_PATH", outputFile.absolutePath)
            }
            startActivity(intent)

        } catch (e: Exception) {
            showError("保存失败: ${e.message}")
        }
    }

    private fun getAdjustedCropRect(): Rect {
        val viewRect = cropView.getCropRect()
        return Rect(
            (viewRect.left / scaleFactor).toInt().coerceIn(0, gifWidth),
            (viewRect.top / scaleFactor).toInt().coerceIn(0, gifHeight),
            (viewRect.right / scaleFactor).toInt().coerceIn(0, gifWidth),
            (viewRect.bottom / scaleFactor).toInt().coerceIn(0, gifHeight)
        )
    }

    private fun createOutputFile(): File {
        // 这里将文件保存到缓存目录
        return File(cacheDir, "cropped_${System.currentTimeMillis()}.gif")
    }

    private fun encodeGif(cropRect: Rect, outputFile: File) {
        val inputPath = getFilePathFromUri(inputGifUri ?: return)
        val outputPath = outputFile.absolutePath
        val cropFilter = "crop=${cropRect.width()}:${cropRect.height()}:${cropRect.left}:${cropRect.top}"

        val command = arrayOf(
            "-i", inputPath,
            "-vf", cropFilter,
            "-y", outputPath
        )

        val returnCode = FFmpeg.execute(command)

        if (returnCode != 0) {
            throw RuntimeException("FFmpeg 裁剪失败: $returnCode")
        }

        // 通知媒体库更新
        MediaScannerConnection.scanFile(this, arrayOf(outputFile.absolutePath), null, null)
    }

    private fun getFilePathFromUri(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("无法读取文件")
        val tempFile = File.createTempFile("input_", ".gif", cacheDir)
        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }
        return tempFile.absolutePath
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showSuccess(file: File) {
        Toast.makeText(this, "保存成功: ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }
}

