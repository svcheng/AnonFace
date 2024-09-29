package com.mobdeve.anonface

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceBlurringActivity : AppCompatActivity() {
    private lateinit var capturedPhoto: ImageView
    private lateinit var faceDetector: FaceDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_face_blurring)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // display captured image in the ImageView
        val uri = Uri.parse(intent.getStringExtra("uri"))
        capturedPhoto = findViewById(R.id.capturedPhoto)
        capturedPhoto.setImageURI(uri)

        // initialize face detector
        val options = FaceDetectorOptions.Builder()
            .setMinFaceSize(0.05f)
            .build()
        faceDetector = FaceDetection.getClient(options)

        val confirmBtn: Button = findViewById(R.id.confirmBtn)
        confirmBtn.setOnClickListener { processImg(uri) }
    }

    private fun processImg(uri: Uri) {
        val img = InputImage.fromFilePath(baseContext, uri)
        val result = faceDetector.process(img)
            .addOnSuccessListener { faces ->
                for (face in faces) {
                    val bounds = face.boundingBox
                }
            }
            .addOnFailureListener { Log.e("", "ML Kit unable to process image") }
    }
}