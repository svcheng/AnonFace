package com.mobdeve.anonface

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
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
    private var galleryUri: Uri? = null

    // Get image from gallery
    private val myActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            try {
                if (result.data != null) {
                    galleryUri = result.data!!.data
                    //Picasso.get().load(galleryUri).into(viewBinding.tempImageIv)
                    val intent = Intent(baseContext, FaceBlurringActivity::class.java)
                    intent.putExtra("uri", galleryUri.toString())
                    startActivity(intent)
                }
            } catch (exception: Exception) {
                Log.d("TAG", "" + exception.localizedMessage)
            }
        }
    }

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


        val takePhotoBtn: ImageButton = findViewById(R.id.takePhotoBtn)
        takePhotoBtn.setOnClickListener { takePhoto() }

        val switchCameraBtn: ImageButton = findViewById(R.id.switchCameraBtn)
        switchCameraBtn.setOnClickListener {
            cameraController.cameraSelector =
                if (cameraController.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
        }

        val galleryBtn : ImageButton = findViewById(R.id.galleryBtn)
        galleryBtn.setOnClickListener {

            val intent: Intent = Intent().apply {
                type = "image/*"
                action = Intent.ACTION_OPEN_DOCUMENT
            }
            myActivityResultLauncher.launch(Intent.createChooser(intent, "Select Picture"))
        }
    }

    private fun takePhoto() {
        val filename = SimpleDateFormat("yyyy:MM:dd/HH:mm:ss", Locale.TAIWAN)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AnonFace-Temp")
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