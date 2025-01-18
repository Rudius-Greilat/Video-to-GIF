// MainActivity.kt
package com.example.video_to_gif

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.arthenica.mobileffmpeg.FFmpeg

class MainActivity : AppCompatActivity() {

    private lateinit var selectVideoButton: Button
    private lateinit var convertToGifButton: Button
    private lateinit var videoPathText: TextView
    private lateinit var outputPathText: TextView
    private lateinit var conversionProgressBar: ProgressBar

    private var selectedVideoUri: Uri? = null
    private var outputGifPath: String? = null

    // 请求存储权限
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    // 视频选择启动器
    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // 获取永久访问权限
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            selectedVideoUri = it
            videoPathText.text = "Video Selected: ${DocumentFile.fromSingleUri(this, it)?.name}"
            convertToGifButton.isEnabled = true
        } ?: run {
            videoPathText.text = "No video selected"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 请求必要的权限
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }

        // UI 初始化代码保持不变...
        // 创建布局容器
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(16, 16, 16, 16)

        // 创建按钮、文本框和进度条
        selectVideoButton = Button(this).apply {
            text = "Select Video"
        }

        convertToGifButton = Button(this).apply {
            text = "Convert to GIF"
            isEnabled = false // 默认不可用
        }

        videoPathText = TextView(this).apply {
            text = "No video selected"
        }

        outputPathText = TextView(this)

        conversionProgressBar = ProgressBar(this).apply {
            visibility = ProgressBar.GONE // 默认不可见
        }

        // 将控件添加到布局中
        layout.addView(selectVideoButton)
        layout.addView(videoPathText)
        layout.addView(convertToGifButton)
        layout.addView(outputPathText)
        layout.addView(conversionProgressBar)

        // 设置主视图
        setContentView(layout)
        // 设置按钮点击事件
        selectVideoButton.setOnClickListener {
            videoPickerLauncher.launch(arrayOf("video/*"))
        }

        convertToGifButton.setOnClickListener {
            selectedVideoUri?.let { uri ->
                convertVideoToGif(uri)
            } ?: Toast.makeText(this, "Please select a video first.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertVideoToGif(videoUri: Uri) {
        conversionProgressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 创建临时输入文件
                val inputFile = File(cacheDir, "temp_input_video")
                contentResolver.openInputStream(videoUri)?.use { input ->
                    FileOutputStream(inputFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // 创建临时输出文件
                val outputFile = File(cacheDir, "temp_output.gif")

                // FFmpeg 命令
                val command = arrayOf(
                    "-i", inputFile.absolutePath,
                    "-vf", "fps=10,scale=320:-1:flags=lanczos",
                    "-y", // 覆盖已存在的文件
                    outputFile.absolutePath
                )

                // 执行转换
                FFmpeg.execute(command)

                // 保存到媒体库
                saveToMediaStore(outputFile)

                withContext(Dispatchers.Main) {
                    conversionProgressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Conversion successful!", Toast.LENGTH_SHORT).show()
                    outputPathText.text = "GIF saved to gallery"
                }

                // 清理临时文件
                inputFile.delete()
                outputFile.delete()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    conversionProgressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Conversion failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveToMediaStore(gifFile: File) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "video_to_gif_${System.currentTimeMillis()}.gif")
            put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                gifFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }
        }
    }
}