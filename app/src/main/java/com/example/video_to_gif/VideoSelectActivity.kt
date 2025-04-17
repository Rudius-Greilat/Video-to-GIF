package com.example.video_to_gif

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VideoSelectActivity : AppCompatActivity() {
    companion object {
        internal const val TAG = "VideoSelectActivity"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var previewButton: Button
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button

    private var selectedVideoUri: Uri? = null
    private var selectedPosition: Int = -1
    private var videoAdapter: VideoAdapter? = null

    // 权限请求回调
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            // Android 13+: 检查视频权限
            permissions[Manifest.permission.READ_MEDIA_VIDEO] == true -> {
                Log.d(TAG, "READ_MEDIA_VIDEO granted")
                setupRecyclerView()
            }
            // Android 10-12: 检查存储权限
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true -> {
                Log.d(TAG, "READ_EXTERNAL_STORAGE granted")
                setupRecyclerView()
            }
            // 权限被拒绝
            else -> {
                val permanentlyDenied = !shouldShowRequestPermissionRationale(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        Manifest.permission.READ_MEDIA_VIDEO
                    else
                        Manifest.permission.READ_EXTERNAL_STORAGE
                )

                if (permanentlyDenied) {
                    showPermissionSettingsDialog()
                } else {
                    showPermissionExplanationDialog()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_select_activity)

        initializeViews()
        checkAndRequestPermissions()
        setupButtons()
    }

    private fun initializeViews() {
        recyclerView = findViewById<RecyclerView>(R.id.recycler_view).apply {
            layoutManager = LinearLayoutManager(this@VideoSelectActivity)
            setHasFixedSize(true)
        }
        previewButton = findViewById(R.id.preview_button)
        confirmButton = findViewById(R.id.confirm_button)
        cancelButton = findViewById(R.id.cancel_button)

        // 初始化时禁用按钮
        //updateButtonStates(false)
    }

    private fun updateButtonStates(enabled: Boolean) {
        previewButton.isEnabled = enabled
        confirmButton.isEnabled = enabled
    }

    private fun setupButtons() {
        // 预览按钮点击事件
        previewButton.setOnClickListener {
            if (selectedVideoUri == null) {
                Toast.makeText(this, "请选择一个视频", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Intent(this, VideoPlayActivity::class.java).apply {
                putExtra("VIDEO_URI", selectedVideoUri.toString())
                startActivity(this)
            }
        }

        // 确认按钮点击事件
        confirmButton.setOnClickListener {
            if (selectedVideoUri == null) {
                Toast.makeText(this, "请选择一个视频", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Intent(this, SettingActivity::class.java).apply {
                putExtra("VIDEO_URI", selectedVideoUri.toString())
                startActivity(this)
            }
            finish()
        }

        // 取消按钮点击事件
        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun checkAndRequestPermissions() {
        when {
            // Android 13+ (API 33+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (hasPermission(Manifest.permission.READ_MEDIA_VIDEO)) {
                    setupRecyclerView()
                } else {
                    requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_VIDEO))
                }
            }
            // Android 10-12 (API 29-32)
            else -> {
                if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    setupRecyclerView()
                } else {
                    requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                }
            }
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要权限")
            .setMessage("为了访问视频文件，我们需要存储权限。授予权限后才能继续使用该功能。")
            .setPositiveButton("授予权限") { _, _ ->
                checkAndRequestPermissions()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun showPermissionSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要权限")
            .setMessage("您已拒绝授予权限，请在设置中手动开启所需权限后重试。")
            .setPositiveButton("去设置") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(this)
        }
    }

    private fun setupRecyclerView() {
        try {
            val videos = fetchVideosFromGallery()

            if (videos.isEmpty()) {
                Toast.makeText(this, "没有找到视频文件", Toast.LENGTH_SHORT).show()
                return
            }

            videoAdapter = VideoAdapter(videos) { uri, position ->
                selectedVideoUri = uri
                selectedPosition = position
                updateButtonStates(true)
            }
            recyclerView.adapter = videoAdapter

            // 如果有之前选中的视频，恢复选中状态
            if (selectedPosition != -1 && selectedVideoUri != null) {
                videoAdapter?.setSelectedPosition(selectedPosition)
                updateButtonStates(true)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView: ${e.message}", e)
            Toast.makeText(this, "加载视频失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 查询全部视频文件
    private fun fetchVideosFromGallery(): List<Uri> {
        val videoList = mutableListOf<Uri>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED
        )

        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    videoList.add(contentUri)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching videos: ${e.message}", e)
        }

        return videoList
    }

    override fun onResume() {
        super.onResume()
        // 检查权限并更新UI
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hasPermission(Manifest.permission.READ_MEDIA_VIDEO)) ||
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE))) {
            // 不需要重新设置适配器，只需要刷新当前适配器
            videoAdapter?.notifyDataSetChanged()
        }
    }
}