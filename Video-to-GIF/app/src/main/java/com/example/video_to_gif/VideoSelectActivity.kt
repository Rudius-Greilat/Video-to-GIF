package com.example.video_to_gif

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VideoSelectActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var previewButton: Button
    private lateinit var cancelButton: Button
    private var selectedVideoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_select_activity) // 使用你的 video_select_activity.xml

        previewButton = findViewById(R.id.preview_button)
        cancelButton = findViewById(R.id.cancel_button)

        // 检查并请求权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        } else {
            // 权限已授予，执行相关操作
            setupRecyclerView()
        }

        // 点击预览选中的视频
        previewButton.setOnClickListener {
            if (selectedVideoUri != null) {
                // 创建 Intent 启动播放界面，并传递视频 URI
                val intent = Intent(this, VideoPlayActivity::class.java).apply {
                    putExtra("VIDEO_URI", selectedVideoUri.toString())  // 传递视频 URI
                }
                startActivity(intent)  // 启动播放界面
            } else {
                Toast.makeText(this, "请先选择一个视频", Toast.LENGTH_SHORT).show()
            }
        }
        // 点击取消返回
        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    // 权限请求的回调方法
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限授予，执行相关操作
                setupRecyclerView()
            } else {
                // 权限未授予，提示用户
                Toast.makeText(this, "权限被拒绝，无法访问相册", Toast.LENGTH_SHORT).show()

                // 检查是否是永久拒绝
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    // 跳转到应用设置页面，让用户手动授予权限
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
            }
        }
    }

    // 使用 Storage Access Framework 获取视频列表
    private fun fetchVideosFromGallery(): List<Uri> {
        val videoList = mutableListOf<Uri>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED
        )

        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                videoList.add(uri)
            }
        }

        return videoList
    }

    // 设置 RecyclerView
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val videos = fetchVideosFromGallery()
        val adapter = VideoAdapter(videos) { uri ->
            selectedVideoUri = uri
        }
        recyclerView.adapter = adapter
    }

    // 使用 Storage Access Framework (SAF) 选择视频
    private val videoSelectLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val videoUri = result.data?.data
            videoUri?.let {
                selectedVideoUri = it
            }
        }
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        videoSelectLauncher.launch(intent)
    }
}
