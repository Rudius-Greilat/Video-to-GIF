package com.example.video_to_gif

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // 声明 ActivityResultLauncher 用于启动 VideoSelectActivity 并处理返回结果
    private val videoSelectLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedVideoUri = result.data?.data // 获取选中的视频 URI
            // 在主页上显示选中结果
            // 例如更新 UI 显示视频路径或名称
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val selectVideoButton = findViewById<Button>(R.id.btn_album)

        // 点击跳转到选择视频页面
        selectVideoButton.setOnClickListener {
            val intent = Intent(this, VideoSelectActivity::class.java)
            // 使用 videoSelectLauncher 启动 VideoSelectActivity
            videoSelectLauncher.launch(intent)
        }
    }
}
