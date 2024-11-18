package com.mobdeve.anonface

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.get
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.graphics.set
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.IOException
import kotlin.math.roundToInt


class FaceBlurringActivity : AppCompatActivity() {
    private lateinit var capturedPhoto: ImageView
    private var boundingBoxes: MutableList<Rect> = mutableListOf()

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
        capturedPhoto = findViewById(R.id.capturedPhoto)
        val intentUri = intent.getStringExtra("uri")
        lateinit var uri: Uri
        if(intentUri != null) {
            uri = Uri.parse(intent.getStringExtra("uri"))
            capturedPhoto.setImageURI(uri)
        }

        // save event
        val saveBtn: Button = findViewById(R.id.saveBtn)
        saveBtn.setOnClickListener {
            // todo: save processed image
            val intent = Intent(this, PhotoCaptureActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }

        // toggle blur slider
        val blurToggle: ImageButton = findViewById(R.id.blurToggle)
        val blurSlider: Slider = findViewById(R.id.blurSlider)
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

        blurSlider.addOnChangeListener { blurSlider, _, _ ->
            // If value of slider == 0, disable save button
            if(blurSlider.value.toInt() == 0) {
                saveBtn.isEnabled = false
            } else {
                saveBtn.isEnabled = true
            }
        }

        // exit button
        val exit: ImageButton = findViewById(R.id.exit)
        exit.setOnClickListener {
            showDialog()
        }

        getFaceBoxes(uri)
    }

    private fun getFaceBoxes(uri: Uri) {
        val failureMsg = "Unable to detect faces"

        // convert image to InputImage
        val image: InputImage
        try {
            image = InputImage.fromFilePath(baseContext, uri)
        } catch (e: IOException) {
            Log.e(null, "Input preprocessing for ML-kit failed")
            finish()
            return
        }

        // initialize face detector
        val options = FaceDetectorOptions.Builder()
            .setMinFaceSize(0.05f)
            .build()
        val faceDetector = FaceDetection.getClient(options)

        faceDetector.process(image)
            .addOnSuccessListener {
                faces ->
                // get bounding boxes of each face
                for (face in faces) {
                    boundingBoxes.add(face.boundingBox)
                }

                // end if no faces detected
                if (faces.size == 0) {
                    Toast.makeText(baseContext, failureMsg, Toast.LENGTH_SHORT).show()
                    finish()
                }

                // perform blurring
                val kernelRadius = 60
                val blurred = blurFacesBoxFilter(uri, kernelRadius)
                capturedPhoto.setImageBitmap(blurred)
            }
            .addOnFailureListener {
                Toast.makeText(baseContext, failureMsg, Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadSourceBitmap(uri: Uri): Bitmap? {
        val tempBitmap: Bitmap
        try {
            if (Build.VERSION.SDK_INT < 28) {
                tempBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri).copy(Bitmap.Config.ARGB_8888, true)
            } else {
                val source = ImageDecoder.createSource(contentResolver, uri)
                // copy needed since decoded bitmap has hardware configuration, which does not allow .getPixel
                tempBitmap = ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
            }
            return tempBitmap
        } catch (e: Exception) {
            Log.e(null, "Unable to load image as bitmap")
            finish()
            return null
        }
    }

    private fun blurFacesBoxFilter(uri: Uri, kernelRadius: Int): Bitmap? {
        val temp = loadSourceBitmap(uri)
        if (temp == null) {
            return null
        }
        val kernelWidth: Int = 2*kernelRadius + 1
        val scaleFactor: Double = 1.0 / (kernelWidth)
        val width = temp.width
        val height = temp.height
        val res = temp.copy(Bitmap.Config.ARGB_8888, true)

        var color1: Int
        var color2: Int
        var newColor: Int
        var sumRed: Int
        var sumGreen: Int
        var sumBlue: Int

//        Toast.makeText(baseContext, "1", Toast.LENGTH_SHORT).show()

        // horizontal pass - set pixels of temp with moving average of res
        for (y in kernelRadius..<height-kernelRadius) {
            // initialize sum
            sumRed = 0
            sumGreen = 0
            sumBlue = 0
            for (i in -kernelRadius..kernelRadius) {
                color1 = res.get(kernelRadius+i, y)
                sumRed += color1.red
                sumGreen += color1.green
                sumBlue += color1.blue
            }
            newColor = Color.argb(res.get(kernelRadius, y).alpha, (sumRed * scaleFactor).roundToInt(), (sumGreen * scaleFactor).roundToInt(), (sumBlue * scaleFactor).roundToInt())
            temp.set(kernelRadius, y, newColor)

            for (x in kernelRadius+1..<width-kernelRadius) {
                color1 = res.get(x-kernelRadius-1, y)
                color2 = res.get(x+kernelRadius, y)
                sumRed = sumRed - color1.red + color2.red
                sumGreen = sumGreen - color1.green + color2.green
                sumBlue = sumBlue - color1.blue + color2.blue

                newColor = Color.argb(res.get(x, y).alpha, (sumRed * scaleFactor).roundToInt(), (sumGreen * scaleFactor).roundToInt(), (sumBlue * scaleFactor).roundToInt())
                temp.set(x, y, newColor)
            }
        }
//        Toast.makeText(baseContext, "2", Toast.LENGTH_SHORT).show()

        // vertical pass - set pixels of res with moving average of temp
        for (x in kernelRadius..<width-kernelRadius) {
            // initialize sum
            sumRed = 0
            sumGreen = 0
            sumBlue = 0
            for (i in -kernelRadius..kernelRadius) {
                color1 = temp.get(x, kernelRadius+i)
                sumRed += color1.red
                sumGreen += color1.green
                sumBlue += color1.blue
            }

            if (contained(x, kernelRadius)) {
                newColor = Color.argb(
                    temp.get(x, kernelRadius).alpha,
                    (sumRed * scaleFactor).roundToInt(),
                    (sumGreen * scaleFactor).roundToInt(),
                    (sumBlue * scaleFactor).roundToInt()
                )
                res.set(x, kernelRadius, newColor)
            }

            for (y in kernelRadius+1..<height-kernelRadius) {
                color1 = temp.get(x, y-kernelRadius-1)
                color2 = temp.get(x, y+kernelRadius)
                sumRed = sumRed - color1.red + color2.red
                sumGreen = sumGreen - color1.green + color2.green
                sumBlue = sumBlue - color1.blue + color2.blue

                if (contained(x, y)) {
                    newColor = Color.argb(
                        temp.get(x, y).alpha,
                        (sumRed * scaleFactor).roundToInt(),
                        (sumGreen * scaleFactor).roundToInt(),
                        (sumBlue * scaleFactor).roundToInt()
                    )
                    res.set(x, y, newColor)
                }
            }
        }
//        Toast.makeText(baseContext, "3", Toast.LENGTH_SHORT).show()
        return res
    }

    private fun contained(x: Int, y: Int): Boolean {
        for (box in boundingBoxes) {
            if (box.contains(x, y)) {
                return true
            }
        }
        return false
    }

    // Material dialog
    private fun showDialog() {
        MaterialAlertDialogBuilder(this, R.style.DialogTheme)
            .setTitle(R.string.blurring_overlay_title)
            .setNegativeButton(R.string.blurring_overlay_keep) { dialog, which -> }
            .setPositiveButton(R.string.blurring_overlay_discard) { dialog, which ->
                val intent = Intent(this, PhotoCaptureActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
            .show()
    }
}


