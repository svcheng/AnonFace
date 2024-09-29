package com.mobdeve.anonface

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.mobdeve.anonface.databinding.ActivityPhotoCaptureBinding
import java.text.SimpleDateFormat
import java.util.Locale

class PhotoCaptureActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityPhotoCaptureBinding
    private lateinit var cameraController: LifecycleCameraController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewBinding = ActivityPhotoCaptureBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // start camera preview
        val previewView: PreviewView = viewBinding.previewView
        cameraController = LifecycleCameraController(baseContext)
        cameraController.bindToLifecycle(this)
        cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        previewView.controller = cameraController

        val takePhotoBtn: Button = findViewById(R.id.takePhotoBtn)
        takePhotoBtn.setOnClickListener { takePhoto() }
    }

    private fun takePhoto() {
        val filename = SimpleDateFormat("yyyy:MM:dd/HH:mm:ss", Locale.TAIWAN)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AnonFace-No-Blur")
        }
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

        cameraController.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e("", "Unable to take photo")
                }
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val intent = Intent(baseContext, FaceBlurringActivity::class.java)
                    intent.putExtra("uri", outputFileResults.savedUri.toString())
                    startActivity(intent)
                }
            }
        )
    }
}