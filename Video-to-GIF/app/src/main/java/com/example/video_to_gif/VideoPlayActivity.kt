package com.example.video_to_gif

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class VideoPlayActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_play)  // 使用你的视频播放界面布局

        // 初始化 VideoView
        videoView = findViewById(R.id.video_view)

        // 获取视频 URI 并设置给 VideoView
        val videoUriString = intent.getStringExtra("VIDEO_URI")
        val videoUri = Uri.parse(videoUriString)

        // 设置视频资源
        videoView.setVideoURI(videoUri)

        // 设置控制器（可以控制播放、暂停、快进等）
        videoView.setMediaController(MediaController(this))

        // 开始播放
        videoView.start()
    }

    override fun onPause() {
        super.onPause()
        // 暂停视频播放
        videoView.pause()
    }

    override fun onResume() {
        super.onResume()
        // 恢复视频播放
        if (!videoView.isPlaying) {
            videoView.start()
        }
    }

    override fun onStop() {
        super.onStop()
        // 释放资源
        videoView.stopPlayback()
    }
}
