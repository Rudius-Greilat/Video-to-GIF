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
import android.view.ViewGroup.LayoutParams
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.arthenica.mobileffmpeg.FFmpeg
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var mainLayout: LinearLayout
    private lateinit var selectVideoButton: Button
    private lateinit var convertToGifButton: Button
    private lateinit var videoPathText: TextView
    private lateinit var progressBar: ProgressBar

    private var selectedVideoUri: Uri? = null
    private var quality = 720 // 默认分辨率
    private var fps = 15 // 默认帧率

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "需要存储权限才能继续", Toast.LENGTH_SHORT).show()
        }
    }

    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            selectedVideoUri = it
            videoPathText.text = "已选择视频: ${DocumentFile.fromSingleUri(this, it)?.name}"
            convertToGifButton.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        requestPermissions()
        setupListeners()
    }

    private fun setupUI() {
        // 创建主布局
        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        }

        // 创建按钮
        selectVideoButton = Button(this).apply {
            text = "选择视频"
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        // 创建文本显示
        videoPathText = TextView(this).apply {
            text = "未选择视频"
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        // 创建转换按钮
        convertToGifButton = Button(this).apply {
            text = "转换为GIF"
            isEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        // 创建进度条
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            max = 100
            visibility = View.GONE
        }

        // 添加所有视图到主布局
        mainLayout.addView(selectVideoButton)
        mainLayout.addView(videoPathText)
        mainLayout.addView(convertToGifButton)
        mainLayout.addView(progressBar)

        setContentView(mainLayout)
    }

    private fun setupListeners() {
        selectVideoButton.setOnClickListener {
            videoPickerLauncher.launch(arrayOf("video/*"))
        }

        convertToGifButton.setOnClickListener {
            selectedVideoUri?.let { uri ->
                convertToGif(uri)
            } ?: Toast.makeText(this, "请先选择视频", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertToGif(videoUri: Uri) {
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 创建临时输入文件
                val inputFile = File(cacheDir, "temp_input_video")
                contentResolver.openInputStream(videoUri)?.use { input ->
                    FileOutputStream(inputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                progressBar.progress = 30

                // 创建临时输出文件
                val outputFile = File(cacheDir, "temp_output.gif")

                // FFmpeg命令
                val command = arrayOf(
                    "-i", inputFile.absolutePath,
                    "-vf", "fps=$fps,scale=${quality}:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse=dither=bayer:bayer_scale=5:diff_mode=rectangle",
                    "-y",
                    outputFile.absolutePath
                )

                FFmpeg.execute(command)
                progressBar.progress = 70

                // 保存到媒体库
                saveToMediaStore(outputFile)
                progressBar.progress = 90

                withContext(Dispatchers.Main) {
                    progressBar.progress = 100
                    progressBar.visibility = View.GONE
                    showResultDialog(outputFile)
                }

                // 清理临时文件
                inputFile.delete()
                outputFile.delete()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "转换失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun showResultDialog(gifFile: File) {
        AlertDialog.Builder(this)
            .setTitle("GIF 生成完成")
            .setMessage("GIF已保存到相册\n文件大小: ${gifFile.length() / 1024}KB")
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
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