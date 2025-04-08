package com.example.video_to_gif

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class GifSelectActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var gifadapter: GifAdapter
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button

    private val gifList = mutableListOf<GifItem>()
    private var selectedGif: GifItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gif_select)

        initViews()
        loadGifFiles()
        setupButton()
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

    private fun loadGifFiles() {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATA
        )

        val selection = "${MediaStore.Images.Media.MIME_TYPE}=?"
        val selectionArgs = arrayOf("image/gif")

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndex(MediaStore.Images.Media.SIZE)
            val pathIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                val size = it.getLong(sizeIndex)
                val path = it.getString(pathIndex)
                gifList.add(GifItem(name, size, path))
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
                val uri = Uri.fromFile(File(it.path))  // 将文件路径转换为 Uri
                val intent = Intent(this, GifCropActivity::class.java)
                intent.putExtra("GIF_URI", uri.toString())
                startActivity(intent)
            } ?: Toast.makeText(this, "请先选择一个GIF", Toast.LENGTH_SHORT).show()
        }
    }
}
