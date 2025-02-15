package com.example.video_to_gif

import android.net.Uri
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import android.widget.TextView
import android.view.View
import com.arthenica.mobileffmpeg.FFmpeg
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import android.widget.Toast
import android.content.Intent

class SettingActivity : AppCompatActivity() {

    private lateinit var startTimeSlider: SeekBar
    private lateinit var endTimeSlider: SeekBar
    private lateinit var frameRateSlider: SeekBar
    private lateinit var resolutionSlider: SeekBar
    private lateinit var generateGifButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var playerView: PlayerView
    private lateinit var cancelButton: Button

    private lateinit var startTimeText: TextView
    private lateinit var endTimeText: TextView
    private lateinit var frameRateText: TextView
    private lateinit var resolutionText: TextView

    private var exoPlayer: ExoPlayer? = null
    private var selectedVideoUri: Uri? = null
    private var currentGifFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // 获取传递过来的视频URI
        selectedVideoUri = intent.getStringExtra("VIDEO_URI")?.let { Uri.parse(it) }

        initializeViews()
        setupPlayer()
        setupSliders()
        setupButtons()
    }

    private fun initializeViews() {
        startTimeSlider = findViewById(R.id.start_time_slider)
        endTimeSlider = findViewById(R.id.end_time_slider)
        frameRateSlider = findViewById(R.id.frame_rate_slider)
        resolutionSlider = findViewById(R.id.resolution_slider)
        generateGifButton = findViewById(R.id.generate_gif_button)
        progressBar = findViewById(R.id.progress_bar)
        playerView = findViewById(R.id.playerView)
        cancelButton = findViewById(R.id.cancel_button)

        startTimeText = findViewById(R.id.start_time_text)
        endTimeText = findViewById(R.id.end_time_text)
        frameRateText = findViewById(R.id.frame_rate_text)
        resolutionText = findViewById(R.id.resolution_text)
    }

    private fun setupPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            // 设置循环播放
            repeatMode = Player.REPEAT_MODE_ALL
        }
        playerView.player = exoPlayer

        selectedVideoUri?.let { uri ->
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
            }
        }
    }

    private fun setupSliders() {
        // 视频加载完成后设置滑杆
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    val duration = exoPlayer?.duration ?: 0

                    // 设置滑杆范围
                    startTimeSlider.max = duration.toInt()
                    endTimeSlider.max = duration.toInt()

                    // 设置初始值
                    startTimeSlider.progress = 0
                    endTimeSlider.progress = duration.toInt()

                    // 更新文本显示
                    updateTimeText(startTimeText, 0)
                    updateTimeText(endTimeText, duration)
                }
            }
        })

        // 开始时间滑杆监听器
        startTimeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 只更新文本显示，不修改进度值
                updateTimeText(startTimeText, progress.toLong())

                // 更新视频位置
                if (fromUser) {
                    exoPlayer?.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 在滑动结束时检查并调整位置
                seekBar?.let {
                    val currentProgress = it.progress
                    val endTime = endTimeSlider.progress

                    if (currentProgress > endTime) {
                        it.progress = endTime
                        updateTimeText(startTimeText, endTime.toLong())
                        exoPlayer?.seekTo(endTime.toLong())
                    }
                }
            }
        })

        // 结束时间滑杆监听器
        endTimeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 只更新文本显示，不修改进度值
                updateTimeText(endTimeText, progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 在滑动结束时检查并调整位置
                seekBar?.let {
                    val currentProgress = it.progress
                    val startTime = startTimeSlider.progress

                    if (currentProgress < startTime) {
                        it.progress = startTime
                        updateTimeText(endTimeText, startTime.toLong())
                    }
                }
            }
        })

        // 帧率滑块监听器
        frameRateSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fps = progress.coerceAtLeast(1)
                frameRateText.text = "帧率: $fps FPS"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 分辨率滑块监听器
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
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        val timeString = when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
            else -> String.format("%02d:%02d", minutes, seconds % 60)
        }

        textView.text = timeString
    }

    private fun setupButtons() {
        generateGifButton.setOnClickListener {
            convertToGif()
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun convertToGif() {
        if (selectedVideoUri == null) {
            Toast.makeText(this, "没有选择视频", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        generateGifButton.isEnabled = false

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
                    "-vf", "fps=$fps,scale=$resolution:-1:flags=lanczos,split[s0][s1];[s0]palettegen=max_colors=256:stats_mode=full[p];[s1][p]paletteuse=dither=bayer:bayer_scale=5:diff_mode=rectangle",
                    "-y",                                // 覆盖输出文件
                    outputFile.absolutePath              // 输出文件
                )

                // 执行转换
                val result = FFmpeg.execute(command)

                if (result == 0) { // 转换成功
                    withContext(Dispatchers.Main) {
                        // 跳转到新的 Activity
                        val intent = Intent(this@SettingActivity, GifPlayActivity::class.java).apply {
                            putExtra("GIF_PATH", outputFile.absolutePath)
                        }
                        startActivity(intent)

                        progressBar.visibility = View.GONE
                        generateGifButton.isEnabled = true
                    }
                } else {
                    throw Exception("FFmpeg command failed with result code: $result")
                }

                // 清理临时文件
                inputFile.delete()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    generateGifButton.isEnabled = true
                    Toast.makeText(this@SettingActivity, "转换失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        // 清理临时 GIF 文件
        currentGifFile?.delete()
    }
}