package com.example.video_to_gif

import android.os.Bundle
import android.widget.SeekBar
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.*
import android.widget.TextView


class SettingActivity : AppCompatActivity() {

    // Define UI components
    private lateinit var startTimeSlider: SeekBar
    private lateinit var endTimeSlider: SeekBar
    private lateinit var frameRateSlider: SeekBar
    private lateinit var resolutionSlider: SeekBar
    private lateinit var generateGifButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var playerView: PlayerView

    // TextViews for showing the values
    private lateinit var startTimeText: TextView
    private lateinit var endTimeText: TextView
    private lateinit var frameRateText: TextView
    private lateinit var resolutionText: TextView

    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the layout using XML, no Compose theme applied
        setContentView(R.layout.activity_setting)  // Use your XML layout

        // Initialize the sliders, button and text views
        startTimeSlider = findViewById(R.id.start_time_slider)
        endTimeSlider = findViewById(R.id.end_time_slider)
        frameRateSlider = findViewById(R.id.frame_rate_slider)
        resolutionSlider = findViewById(R.id.resolution_slider)
        generateGifButton = findViewById(R.id.generate_gif_button)
        progressBar = findViewById(R.id.progress_bar)
        playerView = findViewById(R.id.playerView)

        // TextViews to display values of sliders
        startTimeText = findViewById(R.id.start_time_text)
        endTimeText = findViewById(R.id.end_time_text)
        frameRateText = findViewById(R.id.frame_rate_text)
        resolutionText = findViewById(R.id.resolution_text)

        // Set up ExoPlayer and sliders
        setupPlayer()
        setupSliders()
    }

    private fun setupPlayer() {
        // Initialize ExoPlayer instance
        exoPlayer = ExoPlayer.Builder(this).build()
        // Load a video resource (change as needed)
        val mediaItem = MediaItem.fromUri("android.resource://com.example.myapplication/raw/ab")
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()

        // Attach player to PlayerView
        playerView.player = exoPlayer
    }

    private fun setupSliders() {
        // Set listener for Start Time slider
        startTimeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Update start time in text view
                startTimeText.text = "起始时间: $progress 毫秒"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Set listener for End Time slider
        endTimeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Update end time in text view
                endTimeText.text = "结束时间: $progress 毫秒"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Set listener for Frame Rate slider
        frameRateSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Update frame rate in text view
                frameRateText.text = "帧率: $progress FPS"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Set listener for Resolution slider
        resolutionSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Update resolution in text view
                resolutionText.text = "分辨率: ${progress}x${progress}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Set up Generate GIF button click action
        generateGifButton.setOnClickListener {
            // Show progress bar while generating GIF
            progressBar.isVisible = true
            generateGif()
        }
    }

    private fun generateGif() {
        // Example implementation for generating GIF
        // In a real scenario, you would capture frames from ExoPlayer and create a GIF

        // Simulating the process with a delay
        CoroutineScope(Dispatchers.Main).launch {
            delay(3000)  // Simulating time-consuming GIF generation process
            progressBar.isVisible = false
            // Notify user that the GIF is ready (for example, show a Toast or update UI)
        }
    }

    // Optionally, handle ExoPlayer lifecycle (start and stop)
    override fun onStart() {
        super.onStart()
        exoPlayer?.play()  // Play the video when the activity starts
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause()  // Pause the video when the activity stops
    }

    // Optionally, handle other lifecycle methods such as onPause(), onResume(), etc.
}

