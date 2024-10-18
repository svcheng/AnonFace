package com.mobdeve.anonface

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter

class GalleryAdapter(private val gallery : ArrayList<GalleryDetails>) : Adapter<GalleryViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.recycler_gallery_template, parent, false)

        return GalleryViewHolder(view)
    }

    override fun getItemCount(): Int {
        return gallery.size
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        holder.bindData(gallery[position])
    }
}