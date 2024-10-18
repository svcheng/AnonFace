package com.mobdeve.anonface

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_gallery)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // gallery images sample
        var galleryPhotos : ArrayList<GalleryDetails> = arrayListOf(
            GalleryDetails(R.drawable.gallery_1),
            GalleryDetails(R.drawable.gallery_2),
            GalleryDetails(R.drawable.gallery_3),
            GalleryDetails(R.drawable.gallery_2),
            GalleryDetails(R.drawable.gallery_3),
            GalleryDetails(R.drawable.gallery_1)
        )

        // Recycler gallery
        var galleryRecycler : RecyclerView = findViewById(R.id.galleryRecycler)
        galleryRecycler.adapter = GalleryAdapter(galleryPhotos)
        galleryRecycler.layoutManager = GridLayoutManager(this,3)

        // Back button, finish() activity
    }
}