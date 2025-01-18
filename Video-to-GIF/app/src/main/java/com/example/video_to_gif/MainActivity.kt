package com.example.video_to_gif

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.arthenica.mobileffmpeg.FFmpeg

class MainActivity : AppCompatActivity() {

    private lateinit var selectVideoButton: Button
    private lateinit var convertToGifButton: Button
    private lateinit var videoPathText: TextView
    private lateinit var outputPathText: TextView
    private lateinit var conversionProgressBar: ProgressBar

    private var selectedVideoUri: Uri? = null
    private var outputGifPath: String? = null

    // 定义视频选择启动器
    private val videoPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedVideoUri = uri
            videoPathText.text = "Video Selected: ${uri.path}"
            convertToGifButton.isEnabled = true
        } else {
            videoPathText.text = "No video selected"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            if (selectedVideoUri == null) {
                Toast.makeText(this, "Please select a video first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            convertVideoToGif(selectedVideoUri!!)
        }
    }

    private fun convertVideoToGif(videoUri: Uri) {
        val inputPath = getRealPathFromUri(videoUri)
        if (inputPath == null) {
            Toast.makeText(this, "Failed to get video path.", Toast.LENGTH_SHORT).show()
            return
        }

        // 获取视频所在目录
        val videoFile = File(inputPath)
        val videoDir = videoFile.parentFile

        // 生成 GIF 文件名（保持与视频文件名相同，只是扩展名为 .gif）
        val gifFileName = videoFile.nameWithoutExtension + ".gif"

        // 将 GIF 文件保存到与视频文件相同的目录
        outputGifPath = File(videoDir, gifFileName).absolutePath

        // 显示进度条
        conversionProgressBar.visibility = ProgressBar.VISIBLE

        // 使用 FFmpeg 进行视频到 GIF 的转换
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ffmpegCommand = arrayOf(
                    "-i", inputPath,
                    "-vf", "fps=10,scale=320:-1:flags=lanczos",
                    outputGifPath!!
                )
                FFmpeg.execute(ffmpegCommand)

                // 在主线程更新 UI
                runOnUiThread {
                    conversionProgressBar.visibility = ProgressBar.GONE
                    Toast.makeText(this@MainActivity, "Conversion successful!", Toast.LENGTH_SHORT).show()
                    outputPathText.text = "Output: $outputGifPath"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    conversionProgressBar.visibility = ProgressBar.GONE
                    Toast.makeText(this@MainActivity, "Conversion failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun getRealPathFromUri(uri: Uri): String? {
        // 获取视频文件的真实路径（根据具体实现替换）
        // 这里需要实现从 contentUri 转换为实际文件路径的逻辑
        return uri.path
    }
}