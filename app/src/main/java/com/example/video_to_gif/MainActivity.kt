package com.example.video_to_gif

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    // 启动系统相机拍摄视频，并直接跳转到 VideoSettingActivity
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedVideoUri: Uri? = result.data?.data
            if (selectedVideoUri != null) {
                val intent = Intent(this, SettingActivity::class.java).apply {
                    putExtra("VIDEO_URI", selectedVideoUri.toString()) // 传递 URI
                }
                startActivity(intent) // 直接跳转到 VideoSettingActivity
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val selectVideoButton = findViewById<Button>(R.id.btn_album)
        val settingsButton = findViewById<Button>(R.id.btn_settings)
        val cameraButton = findViewById<Button>(R.id.btn_camera)

        // 拍摄视频
        cameraButton.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            cameraLauncher.launch(intent)
        }

        // 点击跳转到选择视频页面
        selectVideoButton.setOnClickListener {
            val intent = Intent(this, VideoSelectActivity::class.java)
            // 使用 videoSelectLauncher 启动 VideoSelectActivity
            startActivity(intent)
        }

        // 点击跳转到设置页面
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)  // 启动 SettingActivity
        }
    }
}
