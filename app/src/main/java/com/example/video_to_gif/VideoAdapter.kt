package com.example.video_to_gif

import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.util.concurrent.TimeUnit

class VideoAdapter(
    private val videoList: List<Uri>,
    private val onVideoSelected: (Uri, Int) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private var selectedPosition: Int = -1

    fun setSelectedPosition(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.video_item, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val videoUri = videoList[position]
        val context = holder.itemView.context

        // 查询视频信息
        context.contentResolver.query(
            videoUri,
            arrayOf(
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                // 获取视频名称
                val nameIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val name = if (nameIndex != -1) {
                    cursor.getString(nameIndex)
                } else {
                    context.contentResolver.query(videoUri, null, null, null, null)?.use { c ->
                        val displayNameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (c.moveToFirst() && displayNameIndex != -1) {
                            c.getString(displayNameIndex)
                        } else {
                            "未知视频"
                        }
                    } ?: "未知视频"
                }

                // 获取视频大小
                val sizeIndex = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val size = if (sizeIndex != -1) {
                    formatFileSize(cursor.getLong(sizeIndex))
                } else {
                    context.contentResolver.query(videoUri, null, null, null, null)?.use { c ->
                        val fileSizeIndex = c.getColumnIndex(OpenableColumns.SIZE)
                        if (c.moveToFirst() && fileSizeIndex != -1) {
                            formatFileSize(c.getLong(fileSizeIndex))
                        } else {
                            "未知大小"
                        }
                    } ?: "未知大小"
                }

                // 获取视频时长
                val durationIndex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val duration = if (durationIndex != -1) {
                    formatDuration(cursor.getLong(durationIndex))
                } else {
                    "未知时长"
                }

                // 设置视频信息
                holder.videoName.text = name
            }
        }

        // 加载视频缩略图
        Glide.with(context)
            .load(videoUri)
            .into(holder.thumbnail)

        // 设置选中状态
        holder.checkmark.visibility = if (position == selectedPosition) View.VISIBLE else View.GONE

        // 点击事件处理
        holder.itemView.setOnClickListener {
            setSelectedPosition(holder.adapterPosition)
            onVideoSelected(videoUri, holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int = videoList.size

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.video_thumbnail)
        val checkmark: ImageView = itemView.findViewById(R.id.checkmark)
        val videoName: TextView = itemView.findViewById(R.id.video_name)
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.1f KB", size / 1024f)
            size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024f * 1024f))
            else -> String.format("%.1f GB", size / (1024f * 1024f * 1024f))
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }
}