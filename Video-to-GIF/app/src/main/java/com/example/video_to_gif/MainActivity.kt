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
    private lateinit var qualitySeekBar: SeekBar
    private lateinit var fpsSeekBar: SeekBar
    private lateinit var qualityText: TextView
    private lateinit var fpsText: TextView

    private var selectedVideoUri: Uri? = null
    private var quality = 480 // 默认分辨率
    private var fps = 15 // 默认帧率

    // Deepseek: 修改为使用相册选择视频
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val videoUri: Uri? = result.data?.data
            videoUri?.let {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                selectedVideoUri = it
                videoPathText.text = "已选择视频: ${DocumentFile.fromSingleUri(this, it)?.name}"
                enableControls(true)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "权限未授予", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        requestPermissions()
        setupListeners()
    }

    private fun setupUI() {
        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        }

        selectVideoButton = Button(this).apply {
            text = "选择视频"
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        videoPathText = TextView(this).apply {
            text = "未选择视频"
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        qualityText = TextView(this).apply {
            text = "分辨率: ${quality}p"
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        }

        qualitySeekBar = SeekBar(this).apply {
            max = 1080
            progress = quality
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 16)
            }
            isEnabled = false
        }

        fpsText = TextView(this).apply {
            text = "帧率: $fps fps"
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        }

        fpsSeekBar = SeekBar(this).apply {
            max = 30
            progress = fps
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 16)
            }
            isEnabled = false
        }

        convertToGifButton = Button(this).apply {
            text = "转换为GIF"
            isEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 16)
            }
        }

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

        mainLayout.apply {
            addView(selectVideoButton)
            addView(videoPathText)
            addView(qualityText)
            addView(qualitySeekBar)
            addView(fpsText)
            addView(fpsSeekBar)
            addView(convertToGifButton)
            addView(progressBar)
        }

        setContentView(mainLayout)
    }

    private fun setupListeners() {
        // Deepseek: 修改为使用相册选择视频
        selectVideoButton.setOnClickListener {
            openGallery()
        }

        convertToGifButton.setOnClickListener {
            selectedVideoUri?.let { uri ->
                convertToGif(uri)
            } ?: Toast.makeText(this, "请先选择视频", Toast.LENGTH_SHORT).show()
        }

        qualitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                quality = if (progress < 120) 120 else progress
                qualityText.text = "分辨率: ${quality}p"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        fpsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                fps = if (progress < 5) 5 else progress
                fpsText.text = "帧率: $fps fps"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // Deepseek: 新增方法，用于打开相册选择视频
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun enableControls(enabled: Boolean) {
        qualitySeekBar.isEnabled = enabled
        fpsSeekBar.isEnabled = enabled
        convertToGifButton.isEnabled = enabled
    }

    private fun convertToGif(videoUri: Uri) {
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputFile = File(cacheDir, "temp_input_video")
                contentResolver.openInputStream(videoUri)?.use { input ->
                    FileOutputStream(inputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                progressBar.progress = 30

                val outputFile = File(cacheDir, "temp_output.gif")

                val command = arrayOf(
                    "-i", inputFile.absolutePath,
                    "-vf", buildFFmpegFilter(),
                    outputFile.absolutePath
                )

                FFmpeg.execute(command)
                progressBar.progress = 70

                saveToMediaStore(outputFile)
                progressBar.progress = 90

                withContext(Dispatchers.Main) {
                    progressBar.progress = 100
                    progressBar.visibility = View.GONE
                    showResultDialog(outputFile)
                }

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

    private fun buildFFmpegFilter(): String {
        return "fps=$fps,scale=${quality}:-1:flags=lanczos," +
                "split[s0][s1];[s0]palettegen=max_colors=256:stats_mode=full[p];[s1][p]paletteuse=dither=bayer:bayer_scale=5:diff_mode=rectangle"
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