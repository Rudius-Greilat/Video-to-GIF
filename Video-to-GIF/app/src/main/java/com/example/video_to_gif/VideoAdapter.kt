package com.example.video_to_gif

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class VideoAdapter(
    private val videoList: List<Uri>,
    private val onVideoSelected: (Uri) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private var selectedPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.video_item, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val videoUri = videoList[position]

        // 加载视频缩略图
        Glide.with(holder.itemView.context)
            .load(videoUri)
            .into(holder.thumbnail)

        holder.itemView.setOnClickListener {
            // 使用 adapterPosition 来确保获取到当前项的准确位置
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                selectedPosition = currentPosition
                onVideoSelected(videoUri)
                notifyDataSetChanged() // 刷新整个适配器以更新视图
            }
        }

        holder.checkmark.visibility = if (position == selectedPosition) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = videoList.size

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.video_thumbnail)
        val checkmark: ImageView = itemView.findViewById(R.id.checkmark)
    }
}
