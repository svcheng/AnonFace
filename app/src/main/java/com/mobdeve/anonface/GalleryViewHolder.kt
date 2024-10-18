package com.mobdeve.anonface

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val galleryImg: ImageView = itemView.findViewById(R.id.galleryImg)

    fun bindData(galleryDetails: GalleryDetails) {
        galleryImg.background = itemView.resources.getDrawable(galleryDetails.galleryImg)
    }
}