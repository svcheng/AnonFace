package com.mobdeve.anonface

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FaceSelectionActivity : AppCompatActivity() {
    private lateinit var photoToBlur: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_face_selection)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // display captured image in the ImageView
        photoToBlur = findViewById(R.id.photoToBlur)
        val intentUri = intent.getStringExtra("uri")
        lateinit var uri: Uri
        if(intentUri != null) {
            uri = Uri.parse(intent.getStringExtra("uri"))
            photoToBlur.setImageURI(uri)
        }

        // Confirm, go to FaceBlurringActivity screen
        val confirmBtn : Button = findViewById(R.id.confirmBtn)
        confirmBtn.setOnClickListener {
            val intent = Intent(baseContext, FaceBlurringActivity::class.java)
            intent.putExtra("uri", intentUri)
            startActivity(intent)
        }

        // exit button
        val exit: ImageButton = findViewById(R.id.exit)
        exit.setOnClickListener {
            showDialog()
        }

    }

    // Material dialog
    private fun showDialog() {
        MaterialAlertDialogBuilder(this, R.style.DialogTheme)
            .setTitle(R.string.selection_overlay_title)
            .setNegativeButton(R.string.selection_overlay_keep) { dialog, which -> }
            .setPositiveButton(R.string.selection_overlay_discard) { dialog, which ->
                val intent = Intent(this, PhotoCaptureActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
            .show()
    }
}