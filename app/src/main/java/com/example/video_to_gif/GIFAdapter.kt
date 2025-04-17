package com.example.video_to_gif

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class GifAdapter(
    private val gifList: List<GifItem>,
    private val onItemClick: (GifItem) -> Unit
) : RecyclerView.Adapter<GifAdapter.GifViewHolder>() {

    private var selectedGif: GifItem? = null
    private var selectedPosition: Int = -1

    fun setSelectedGif(gif: GifItem?) {
        selectedGif = gif
        notifyDataSetChanged()
    }

    inner class GifViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.video_name)
        private val size: TextView = view.findViewById(R.id.video_size)
        private val thumbnail: ImageView = view.findViewById(R.id.video_thumbnail)
        private val checkmark: ImageView = view.findViewById(R.id.checkmark)

        fun bind(gif: GifItem) {
            name.text = gif.name
            size.text = "${gif.size / 1024} KB"
            Glide.with(itemView.context)
                .asGif()
                .load(Uri.parse(gif.uri))
                .into(thumbnail)

            val isSelected = gif == selectedGif
            checkmark.visibility = if (isSelected) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onItemClick(gif) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GifViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.gif_item, parent, false)
        return GifViewHolder(view)
    }

    override fun onBindViewHolder(holder: GifViewHolder, position: Int) {
        holder.bind(gifList[position])
    }

    override fun getItemCount(): Int = gifList.size
}
