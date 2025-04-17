package com.example.video_to_gif

import android.Manifest
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts

class GifSelectActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var gifadapter: GifAdapter
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button

    private val gifList = mutableListOf<GifItem>()
    private var selectedGif: GifItem? = null

    companion object {
        private const val TAG = "GifSelectActivity"
        private const val REQUEST_PERMISSION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gif_select)

        initViews()
        setupButton()
        checkAndRequestGifPermissions()

    }

    private fun initViews() {
        gifadapter = GifAdapter(gifList) { gif ->
            // 单选逻辑
            selectedGif = if (selectedGif == gif) null else gif
            gifadapter.setSelectedGif(selectedGif)
        }

        recyclerView = findViewById<RecyclerView?>(R.id.recycler_view).apply {
            layoutManager = LinearLayoutManager(this@GifSelectActivity)
            adapter = gifadapter
        }

        confirmButton = findViewById(R.id.confirm_button)
        cancelButton = findViewById(R.id.cancel_button)
    }

    private fun setupGifRecyclerView() {
        gifList.clear()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE
        )

        val selection = "${MediaStore.Images.Media.MIME_TYPE}=?"
        val selectionArgs = arrayOf("image/gif")

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val cursor = contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex)
                val size = it.getLong(sizeIndex)
                val contentUri = ContentUris.withAppendedId(collection, id)

                gifList.add(GifItem(name, size, contentUri.toString()))
            }

            gifadapter.notifyDataSetChanged()
        }
    }


    private fun setupButton() {
        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        confirmButton.setOnClickListener {
            selectedGif?.let {
                val uri = Uri.parse(it.uri)  // 将文件路径转换为 Uri
                val intent = Intent(this, GifCropActivity::class.java)
                intent.putExtra("GIF_URI", uri.toString())
                startActivity(intent)
            } ?: Toast.makeText(this, "请先选择一个GIF", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true ||
                    permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true -> {
                setupGifRecyclerView()
            }
            else -> {
                val deniedPermanently = !shouldShowRequestPermissionRationale(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        Manifest.permission.READ_MEDIA_IMAGES
                    else
                        Manifest.permission.READ_EXTERNAL_STORAGE
                )

                if (deniedPermanently) {
                    showPermissionSettingsDialog()
                } else {
                    showPermissionExplanationDialog()
                }
            }
        }
    }


    private fun checkAndRequestGifPermissions() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (hasPermission(Manifest.permission.READ_MEDIA_IMAGES)) {
                    setupGifRecyclerView()
                } else {
                    requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                }
            }
            else -> {
                if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    setupGifRecyclerView()
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
            .setMessage("为了访问GIF文件，我们需要图片读取权限。请授予权限后继续。")
            .setPositiveButton("授予权限") { _, _ -> checkAndRequestGifPermissions() }
            .setNegativeButton("取消") { dialog, _ -> dialog.dismiss(); finish() }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要权限")
            .setMessage("您已拒绝授予权限，请前往设置手动开启权限。")
            .setPositiveButton("去设置") { _, _ -> openAppSettings() }
            .setNegativeButton("取消") { dialog, _ -> dialog.dismiss(); finish() }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(this)
        }
    }
}
