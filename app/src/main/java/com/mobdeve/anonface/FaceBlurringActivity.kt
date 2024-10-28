package com.mobdeve.anonface

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
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt


class FaceBlurringActivity : AppCompatActivity() {
    private lateinit var capturedPhoto: ImageView
    private var boundingBoxes: MutableList<Rect> = mutableListOf()
    private lateinit var sourceImageBitmap: Bitmap

    private val sigma: Double = 1.0
    private val kernelRadius: Int = 2
    private val kernelWidth: Int = 2*kernelRadius + 1
    private val kernel: MutableList<Double> = MutableList(kernelWidth * kernelWidth) { 1.0 }

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
        val image = intent.getIntExtra("image", 0)
        lateinit var uri: Uri
        if(intentUri != null) {
            uri = Uri.parse(intent.getStringExtra("uri"))
            capturedPhoto.setImageURI(uri)
        } else if (image != 0) {
            capturedPhoto.setImageResource(image)
        }

        // save event
        val saveBtn: Button = findViewById(R.id.saveBtn)
        saveBtn.setOnClickListener {
            // todo: save processed image

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

        // initialize kernel

        var kernelSum = 0.0
        for (i in 0..<kernelWidth) {
            for (j in 0..<kernelWidth) {
                val g = gaussian2d(i, j)
                kernel[i*kernelWidth + j] = g
                kernelSum += g
            }
        }
        // normalize values
        kernel.map { x -> x / kernelSum }

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
                loadSourceBitmap(uri)
                val blurred = blurFaces()// kernel width of 2*radius + 1
                capturedPhoto.setImageBitmap(blurred)
            }
            .addOnFailureListener {
                Toast.makeText(baseContext, failureMsg, Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadSourceBitmap(uri: Uri) {
        try {
            if (Build.VERSION.SDK_INT < 28) {
                sourceImageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(contentResolver, uri)
                // copy needed since decoded bitmap has hardware configuration, which does not allow .getPixel
                sourceImageBitmap = ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, false)
            }
        } catch (e: Exception) {
            Log.e(null, "Unable to load image as bitmap")
            finish()
        }
    }

    private fun blurFaces(): Bitmap {
        val res = sourceImageBitmap.copy(Bitmap.Config.ARGB_8888, true)

        for (x in kernelRadius..<res.width-kernelRadius) {
            for (y in kernelRadius..<res.height-kernelRadius) {
                var contained = false
                for (box in boundingBoxes) {
                    if (box.contains(x, y)) {
                        contained = true
                        break
                    }
                }

                if (!contained) {
                    res.set(x, y, sourceImageBitmap.get(x, y))
                    continue
                }

                val newColor = gaussianConvolution(sourceImageBitmap, x, y)
                res.set(x, y, newColor)
            }
        }

        return res
    }

    private fun gaussian2d(x: Int, y: Int): Double {
        val x2 = x.toDouble().pow(2)
        val y2 = y.toDouble().pow(2)
        val sigma2 = sigma.pow(2)
        val numerator = exp(-(x2 + y2) / sigma2)
        val denominator = 2 * PI * sigma2
        return numerator / denominator
    }

    private fun gaussianConvolution(origBitmap: Bitmap, x: Int, y: Int): Int {
        var sumRed = 0.0
        var sumGreen = 0.0
        var sumBlue = 0.0

        // compute convolution
        val kernelCenter: Int = kernelWidth / 2
        for (i in -kernelRadius..kernelRadius) {
            for (j in -kernelRadius..kernelRadius) {
                val kernelVal = kernel[(kernelCenter + i) * kernelWidth + (kernelCenter + j)]

                val oldRed = origBitmap.get(x+i, y+i).red
                val oldGreen = origBitmap.get(x+i, y+i).green
                val oldBlue = origBitmap.get(x+i, y+i).blue

                sumRed += oldRed * kernelVal
                sumGreen += oldGreen * kernelVal
                sumBlue += oldBlue * kernelVal
            }
        }

        return Color.argb(origBitmap.get(x, y).alpha, sumRed.roundToInt(), sumGreen.roundToInt(), sumBlue.roundToInt())
    }

    // Material dialog
    private fun showDialog() {
        MaterialAlertDialogBuilder(this, R.style.DialogTheme)
            .setTitle(R.string.blurring_overlay_title)
            .setNegativeButton(R.string.blurring_overlay_keep) { dialog, which -> }
            .setPositiveButton(R.string.blurring_overlay_discard) { dialog, which ->
                finish()
            }
            .show()
    }
}


