package com.example.video_to_gif

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileInputStream
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Intent
import com.bumptech.glide.load.engine.DiskCacheStrategy

class GifPlayActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "GifPlayActivity"
    }

    private lateinit var gifPreview: ImageView
    private lateinit var cancelButton: Button
    private lateinit var returnButton: Button
    private lateinit var saveButton: Button

    private var gifFile: File? = null

    // 权限请求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true -> {
                Log.d(TAG, "Storage permission granted")
                saveGifWithPermission()
            }
            else -> {
                Log.e(TAG, "Storage permission denied")
                Toast.makeText(this, "需要存储权限才能保存GIF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gif_paly)

        Log.d(TAG, "GifPlayActivity onCreate")
        initializeViews()

        val gifPath = intent.getStringExtra("GIF_PATH")
        Log.d(TAG, "Received GIF path: $gifPath")

        if (gifPath != null) {
            gifFile = File(gifPath)
            if (gifFile?.exists() == true) {
                Log.d(TAG, "GIF file exists at path: ${gifFile?.absolutePath}")
                playGif()
            } else {
                Log.e(TAG, "GIF file does not exist at path: $gifPath")
                Toast.makeText(this, "GIF 文件不存在", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Log.e(TAG, "No GIF path received")
            Toast.makeText(this, "GIF 文件路径无效", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeViews() {
        gifPreview = findViewById(R.id.gifPreview)
        cancelButton = findViewById(R.id.cancel_button)
        returnButton = findViewById(R.id.return_button)
        saveButton = findViewById(R.id.save_button)

        //保存按钮：保存GIF
        saveButton.setOnClickListener {
            checkAndRequestPermissions()
        }

        //取消按钮：返回上一页
        cancelButton.setOnClickListener {
            finish()
        }

        // 返回按钮：直接返回主页
        returnButton.setOnClickListener {
            val intnet = Intent(this,MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intnet)
        }
    }

    // 加载 GIF 图片
    /*2025.3.20，解决Glide图片缓存导致的显示错误问题*/
    private fun playGif() {
        gifFile?.let { file ->
            Log.d(TAG, "Loading GIF into preview")
            Glide.with(this)
                .asGif()
                .load(file)
                .skipMemoryCache(true)                  // 跳过内存缓存
                .diskCacheStrategy(DiskCacheStrategy.NONE)   // 禁止磁盘缓存
                .into(gifPreview)
        }
    }

    // 检查储存权限
    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 及以上不需要请求存储权限
            saveGifWithPermission()
        } else {
            // Android 9 及以下需要请求存储权限
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }


    private fun saveGifWithPermission() {
        Log.d(TAG, "Starting to save GIF")
        gifFile?.let { file ->
            try {
                Log.d(TAG, "GIF file size: ${file.length()} bytes")
                val filename = "gif_${System.currentTimeMillis()}.gif"

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VideoToGif")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                Log.d(TAG, "ContentValues prepared: $contentValues")

                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                Log.d(TAG, "Using collection URI: $collection")

                val imageUri = contentResolver.insert(collection, contentValues)
                Log.d(TAG, "Inserted URI: $imageUri")

                if (imageUri != null) {
                    contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                        Log.d(TAG, "Got output stream, starting to copy file")
                        FileInputStream(file).use { inputStream ->
                            val bytesCopied = inputStream.copyTo(outputStream)
                            Log.d(TAG, "Copied $bytesCopied bytes")
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        contentResolver.update(imageUri, contentValues, null, null)
                        Log.d(TAG, "Updated IS_PENDING to 0")
                    }

                    Toast.makeText(this, "GIF 已保存到相册的 VideoToGif 文件夹", Toast.LENGTH_LONG).show()
                    Log.d(TAG, "Save completed successfully")
                } else {
                    Log.e(TAG, "Failed to create new MediaStore entry")
                    Toast.makeText(this, "保存失败：无法创建文件", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving GIF", e)
                Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Log.e(TAG, "No GIF file to save")
            Toast.makeText(this, "没有可保存的GIF文件", Toast.LENGTH_SHORT).show()
        }
    }
}