package com.example.video_to_gif

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.widget.SeekBar
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import android.widget.TextView
import com.arthenica.mobileffmpeg.FFmpeg
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import android.widget.Toast
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable

class SettingActivity : AppCompatActivity() {

    private lateinit var startTimeSlider: SeekBar
    private lateinit var endTimeSlider: SeekBar
    private lateinit var frameRateSlider: SeekBar
    private lateinit var resolutionSlider: SeekBar
    private lateinit var playerView: PlayerView

    private lateinit var generateGifButton: Button
    private lateinit var returnButton: Button
    private lateinit var cancelButton: Button

    private lateinit var startTimeText: TextView
    private lateinit var endTimeText: TextView
    private lateinit var frameRateText: TextView
    private lateinit var resolutionText: TextView

    private var exoPlayer: ExoPlayer? = null
    private var selectedVideoUri: Uri? = null
    private var currentGifFile: File? = null
    private var isUserSeeking = false

    // 新增：加载对话框
    private lateinit var progressDialog: Dialog
    private lateinit var dialogProgressBar: ProgressBar
    private lateinit var dialogProgressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // 获取传递过来的视频 URI
        selectedVideoUri = intent.getStringExtra("VIDEO_URI")?.let { Uri.parse(it) }

        initializeViews()
        setupPlayer()
        setupSliders()
        loadSettings() // 加载设置
        setupButtons()

        // 初始化加载对话框
        initProgressDialog()
    }

    private fun initializeViews() {
        startTimeSlider = findViewById(R.id.start_time_slider)
        endTimeSlider = findViewById(R.id.end_time_slider)
        frameRateSlider = findViewById(R.id.frame_rate_slider)
        resolutionSlider = findViewById(R.id.resolution_slider)
        playerView = findViewById(R.id.playerView)

        generateGifButton = findViewById(R.id.generate_gif_button)
        cancelButton = findViewById(R.id.cancel_button)
        returnButton = findViewById(R.id.return_button)

        startTimeText = findViewById(R.id.start_time_text)
        endTimeText = findViewById(R.id.end_time_text)
        frameRateText = findViewById(R.id.frame_rate_text)
        resolutionText = findViewById(R.id.resolution_text)
    }

    private fun setupPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = false  // 默认不自动播放
        }
        playerView.player = exoPlayer

        selectedVideoUri?.let { uri ->
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY && !isUserSeeking) {
                            val duration = duration
                            if (!isUserSeeking) {
                                startTimeSlider.max = duration.toInt()
                                endTimeSlider.max = duration.toInt()
                                if (endTimeSlider.progress == 0) {
                                    endTimeSlider.progress = duration.toInt()
                                }
                                if (startTimeText.text.isEmpty()) {
                                    updateTimeText(startTimeText, 0)
                                }
                                if (endTimeText.text.isEmpty()) {
                                    updateTimeText(endTimeText, duration)
                                }
                            }
                        }
                    }

                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        if (reason != Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
                            pause()
                        }
                    }
                })
            }
        }
    }

    // 设置滑动条，绑定参数
    private fun setupSliders() {
        var lastStartProgress = 0
        var lastEndProgress = 0

        startTimeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser && !isUserSeeking) return

                val actualProgress = if (progress >= endTimeSlider.progress - 1000) {
                    endTimeSlider.progress - 1000
                } else {
                    progress
                }

                lastStartProgress = actualProgress
                seekBar?.progress = actualProgress
                updateTimeText(startTimeText, actualProgress.toLong())

                if (fromUser) {
                    exoPlayer?.seekTo(actualProgress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
                exoPlayer?.playWhenReady = false  // 拖动时暂停播放
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                updateTimeText(startTimeText, lastStartProgress.toLong())
            }
        })

        endTimeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser && !isUserSeeking) return

                val actualProgress = if (progress <= startTimeSlider.progress + 1000) {
                    startTimeSlider.progress + 1000
                } else {
                    progress
                }

                lastEndProgress = actualProgress
                seekBar?.progress = actualProgress
                updateTimeText(endTimeText, actualProgress.toLong())

                if (fromUser) {
                    exoPlayer?.seekTo(actualProgress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
                exoPlayer?.playWhenReady = false  // 拖动时暂停播放
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                updateTimeText(endTimeText, lastEndProgress.toLong())
            }
        })

        // 帧率滑块监听器保持不变
        frameRateSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fps = progress.coerceAtLeast(1)
                frameRateText.text = "帧率: $fps FPS"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 分辨率滑块监听器保持不变
        resolutionSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val resolution = progress.coerceAtLeast(120)
                resolutionText.text = "分辨率: ${resolution}p"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 设置初始值
        frameRateSlider.progress = 15
        resolutionSlider.progress = 480
    }

    // 格式化时间显示
    private fun updateTimeText(textView: TextView, milliseconds: Long) {
        val totalSeconds = milliseconds / 1000
        val ms = milliseconds % 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hours = minutes / 60

        val timeString = when {
            hours > 0 -> String.format("%02d:%02d:%02d.%03d", hours, minutes % 60, seconds, ms)
            else -> String.format("%02d:%02d.%03d", minutes, seconds, ms)
        }

        textView.text = timeString
    }


    // 设置按钮监听器
    private fun setupButtons() {
        generateGifButton.setOnClickListener {
            saveSettings()
            convertToGif()
        }

        returnButton.setOnClickListener {
            saveSettings()
            val intent = Intent(this,MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        cancelButton.setOnClickListener {
            saveSettings()
            finish()
        }
    }

    // 初始化进度框
    private fun initProgressDialog() {
        progressDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_progress)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(false)
        }

        dialogProgressBar = progressDialog.findViewById(R.id.loading_progress_bar)
        dialogProgressText = progressDialog.findViewById(R.id.progress_text)
    }

    // Video转换GIF
    private fun convertToGif() {
        // 检查视频Uri
        if (selectedVideoUri == null) {
            Toast.makeText(this, "没有选择视频", Toast.LENGTH_SHORT).show()
            return
        }

        // 显示加载对话框
        progressDialog.show()

        CoroutineScope(Dispatchers.Main).launch {
            // 模拟进度更新，从 0% 到 100%
            for (progress in 0..100) {
                dialogProgressText.text = "$progress%"
                delay(5) // 每次更新延迟 50ms，可调整
            }
        }


        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 将视频文件复制到临时文件
                val inputFile = File(cacheDir, "temp_input_video")
                contentResolver.openInputStream(selectedVideoUri!!)?.use { input ->
                    FileOutputStream(inputFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val outputFile = File(cacheDir, "temp_output.gif")
                currentGifFile = outputFile

                // 获取设置的参数
                val fps = frameRateSlider.progress.coerceAtLeast(1)
                val resolution = resolutionSlider.progress.coerceAtLeast(120)

                // 获取开始和结束时间（秒）
                val startTime = (startTimeSlider.progress / 1000f).toString()
                val duration = ((endTimeSlider.progress - startTimeSlider.progress) / 1000f).toString()

                // 构建 FFmpeg 命令，添加时间范围参数
                val command = arrayOf(
                    "-ss", startTime,                    // 开始时间
                    "-t", duration,                      // 持续时间
                    "-i", inputFile.absolutePath,        // 输入文件
                    "-vf", "fps=$fps,scale=$resolution:-1:flags=lanczos,split[s0][s1];[s0]palettegen=max_colors=128:stats_mode=full[p];[s1][p]paletteuse=dither=none:diff_mode=rectangle",
                    "-y",                                // 覆盖输出文件
                    outputFile.absolutePath              // 输出文件
                )
                // GIF颜色最大支持数max_color 256 -> 128
                // 抖动算法，用于平滑颜色过渡区域 dither bayer:bayer_scale=5 -> none

                // 执行转换
                val result = FFmpeg.execute(command)

                if (result == 0) { // 转换成功
                    withContext(Dispatchers.Main) {
                        // 跳转到新的 Activity
                        val intent = Intent(this@SettingActivity, GifPlayActivity::class.java).apply {
                            putExtra("GIF_PATH", outputFile.absolutePath)
                        }
                        startActivity(intent)

                        // 隐藏加载对话框
                        progressDialog.dismiss()
                    }
                } else {
                    throw Exception("FFmpeg command failed with result code: $result")
                }

                // 清理临时文件
                inputFile.delete()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@SettingActivity, "转换失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("gif_settings", MODE_PRIVATE)
        with(prefs.edit()) {
            putInt("start_time", startTimeSlider.progress)
            putInt("end_time", endTimeSlider.progress)
            putInt("frame_rate", frameRateSlider.progress)
            putInt("resolution", resolutionSlider.progress)
            apply()
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("gif_settings", MODE_PRIVATE)
        startTimeSlider.progress = prefs.getInt("start_time", 0)
        endTimeSlider.progress = prefs.getInt("end_time", endTimeSlider.max) // 默认设为最大值
        frameRateSlider.progress = prefs.getInt("frame_rate", 15)
        resolutionSlider.progress = prefs.getInt("resolution", 480)

        // 更新文本显示
        updateTimeText(startTimeText, startTimeSlider.progress.toLong())
        updateTimeText(endTimeText, endTimeSlider.progress.toLong())
        frameRateText.text = "帧率: ${frameRateSlider.progress.coerceAtLeast(1)} FPS"
        resolutionText.text = "分辨率: ${resolutionSlider.progress.coerceAtLeast(120)}p"
    }



    override fun onDestroy() {
        saveSettings()
        super.onDestroy()
        exoPlayer?.release()
        // 清理临时 GIF 文件
        currentGifFile?.delete()
    }
}