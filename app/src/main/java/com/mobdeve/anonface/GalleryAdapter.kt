package com.mobdeve.anonface

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter

class GalleryAdapter(private val gallery : ArrayList<GalleryDetails>, private val context: Context) : Adapter<GalleryViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.recycler_gallery_template, parent, false)

        return GalleryViewHolder(view)
    }

    override fun getItemCount(): Int {
        return gallery.size
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        //holder.bindData(gallery[position])
        val galleryList = gallery[position]
        holder.galleryImg.setImageResource(galleryList.galleryImg)

        // clicking on an image should direct them to FaceBlurringActivity
        holder.itemView.setOnClickListener {
            val intent = Intent(context, FaceBlurringActivity::class.java).apply {
                putExtra("image",galleryList.galleryImg)
            }
            context.startActivity(intent)
        }
    }

}