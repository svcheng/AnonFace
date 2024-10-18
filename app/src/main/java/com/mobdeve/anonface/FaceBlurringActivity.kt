package com.mobdeve.anonface

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
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

        val saveBtn: Button = findViewById(R.id.saveBtn)
        saveBtn.setOnClickListener { processImg(uri) }

        // toggle blur slider
        var blurToggle: ImageButton = findViewById(R.id.blurToggle)
        var blurSlider: Slider = findViewById(R.id.blurSlider)
        var click = 0 // 0 -> show slider; 1 -> hide slider
        blurToggle.setOnClickListener {
            // set slider to visible, blurToggle to selected state
                if(click == 0) {
                    blurSlider.visibility = Slider.VISIBLE
                    blurToggle.setBackgroundResource(R.drawable.blur_selected)
                    click = 1
                } else { // set slider to invisible, blurToggle to deselected
                    blurSlider.visibility = Slider.INVISIBLE
                    blurToggle.setBackgroundResource(R.drawable.blur_deselected)
                    click = 0
                }
        }
        // unblur button
        // TODO: set disabled if slider value == 0, button design should be outline; if enabled: button is filled

        // exit button
        // TODO: put overlay asking to discard or keep changes
        // OPTIONS: keep editing (stay in activity) or discard (go back to camera)
        var exit: ImageButton = findViewById(R.id.exit)
        exit.setOnClickListener{
            MaterialAlertDialogBuilder(baseContext)
                .setTitle(resources.getString(R.string.blurring_overlay_title))
                .setNegativeButton(resources.getString(R.string.blurring_overlay_keep)) { dialog, which ->
                    // Respond to negative button press
                }
                .setPositiveButton(resources.getString(R.string.blurring_overlay_discard)) { dialog, which ->
                    // Respond to positive button press
                }
                .show()
        }

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