package com.mobdeve.anonface

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt


class FaceBlurringActivity : AppCompatActivity() {
    private lateinit var capturedPhoto: ImageView
    private var boundingBoxes: MutableList<Rect> = mutableListOf()
    private var blurredPhoto: Bitmap? = null
    private lateinit var blurSlider: Slider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_face_blurring)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        capturedPhoto = findViewById(R.id.capturedPhoto)
        val blurBtn: Button = findViewById(R.id.blurBtn)
        val saveBtn: Button = findViewById(R.id.saveBtn)
        blurSlider = findViewById(R.id.blurSlider)

        // display captured image in the ImageView
        val intentUri = intent.getStringExtra("uri")
        lateinit var uri: Uri
        if (intentUri != null) {
            uri = Uri.parse(intent.getStringExtra("uri"))
            capturedPhoto.setImageURI(uri)
        }

        // blur event
        blurBtn.setOnClickListener {
            blurredPhoto = blurFacesBoxFilter(uri)
            if (blurredPhoto != null) {
                capturedPhoto.setImageBitmap(blurredPhoto)
                saveBtn.isEnabled = true
            }
        }

        // save event
        saveBtn.setOnClickListener {
            // todo: save processed
            if (blurredPhoto != null) {
                val filename = "blurred_" + SimpleDateFormat("yyyy:MM:dd/HH:mm:ss", Locale.TAIWAN)
                    .format(System.currentTimeMillis()) + ".png"

                saveImage(blurredPhoto!!, filename)
                finish()
            }
        }

        // blur slider
        blurSlider.setValueTo(200.0f)
        blurSlider.setValueFrom(15.0f)
        blurSlider.value = 80.0f

        // exit button
        val exit: ImageButton = findViewById(R.id.exit)
        exit.setOnClickListener {
            showDialog()
        }

        // invoke ML-kit to get face bounding boxes
        getFaceBoxes(uri)
    }

    private fun getContentValues() : ContentValues {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        return values
    }

    private fun saveToStream(outputStream: OutputStream?, bitmap: Bitmap) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveImage(bitmap: Bitmap, fileName: String) {
        val folderName = "AnonFace"
        val contentValues = getContentValues()

        if (Build.VERSION.SDK_INT >= 29) {
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folderName")
            contentValues.put(MediaStore.Images.Media.IS_PENDING, true)

            val destUri: Uri? = applicationContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (destUri != null) {
                saveToStream(applicationContext.contentResolver.openOutputStream(destUri), bitmap)
                contentValues.put(MediaStore.Images.Media.IS_PENDING, false)
                applicationContext.contentResolver.update(destUri, contentValues, null, null)
            }
        } else {
            val directory = File(Environment.getExternalStorageDirectory().toString() + "/" + folderName)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, fileName)

            saveToStream(FileOutputStream(file), bitmap)
            contentValues.put(MediaStore.Images.Media.DATA, file.absolutePath)
            applicationContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        }
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
            .setMinFaceSize(0.03f)
            .build()
        val faceDetector = FaceDetection.getClient(options)

        try {
            // invoke face detector asynchronously and await its completion
            val task = faceDetector.process(image)
            while (!task.isComplete) {
                continue
            }
            val faces = task.result

            // end if no faces detected
            if (faces.size == 0) {
                Toast.makeText(baseContext, failureMsg, Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            for (face in faces) {
                boundingBoxes.add(face.boundingBox)
            }
        } catch (e: Exception) {
            Toast.makeText(baseContext, failureMsg, Toast.LENGTH_SHORT).show()
            finish()
        } finally {
            faceDetector.close()
        }
    }

    private fun loadSourceBitmap(uri: Uri): Bitmap? {
        val tempBitmap: Bitmap
        try {
            if (Build.VERSION.SDK_INT < 28) {
                tempBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri).copy(Bitmap.Config.ARGB_8888, true)
            } else {
                val source = ImageDecoder.createSource(contentResolver, uri)
                // copy needed since decoded bitmap has hardware configuration, which does not allow .getPixel() to be called
                tempBitmap = ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
            }
            return tempBitmap
        } catch (e: Exception) {
            Log.e(null, "Unable to load image as bitmap")
            finish()
            return null
        }
    }

    private fun blurFacesBoxFilter(uri: Uri): Bitmap? {
        val temp = loadSourceBitmap(uri) ?: return null
        val out = temp.copy(Bitmap.Config.ARGB_8888, true)

        val kernelRadius = blurSlider.value.toInt()
        val kernelWidth: Int = 2*kernelRadius + 1
        val scaleFactor: Double = 1.0 / (kernelWidth)
        val width = temp.width
        val height = temp.height

        // temporary variables
        var color1: Int
        var color2: Int
        var newColor: Int
        var sumRed: Int
        var sumGreen: Int
        var sumBlue: Int

        // horizontal pass - set pixels of temp with moving average of out's pixels
        for (y in kernelRadius..<height-kernelRadius) {
            // initialize sum with the first pixel in the row
            sumRed = 0
            sumGreen = 0
            sumBlue = 0
            // compute average of pixels in kernel window
            for (i in -kernelRadius..kernelRadius) {
                color1 = out.get(kernelRadius+i, y)
                sumRed += color1.red
                sumGreen += color1.green
                sumBlue += color1.blue
            }
            newColor = Color.argb(
                out.get(kernelRadius, y).alpha,
                (sumRed * scaleFactor).roundToInt(),
                (sumGreen * scaleFactor).roundToInt(),
                (sumBlue * scaleFactor).roundToInt()
            )
            temp.set(kernelRadius, y, newColor)

            // set the other pixels in the row, tracking the local sum
            for (x in kernelRadius+1..<width-kernelRadius) {
                color1 = out.get(x-kernelRadius-1, y) // pixel that left the window
                color2 = out.get(x+kernelRadius, y) // pixel that entered the window
                sumRed = sumRed - color1.red + color2.red
                sumGreen = sumGreen - color1.green + color2.green
                sumBlue = sumBlue - color1.blue + color2.blue

                newColor = Color.argb(
                    out.get(x, y).alpha,
                    (sumRed * scaleFactor).roundToInt(),
                    (sumGreen * scaleFactor).roundToInt(),
                    (sumBlue * scaleFactor).roundToInt()
                )
                temp.set(x, y, newColor)
            }
        }

        // vertical pass - set pixels of out with moving average of temp's pixels
        for (x in kernelRadius..<width-kernelRadius) {
            // initialize sum with the first pixel in the column
            sumRed = 0
            sumGreen = 0
            sumBlue = 0
            // compute average of pixels in kernel window
            for (i in -kernelRadius..kernelRadius) {
                color1 = temp.get(x, kernelRadius+i)
                sumRed += color1.red
                sumGreen += color1.green
                sumBlue += color1.blue
            }
            // only modify pixel value if pixel is part of a face
            if (contained(x, kernelRadius)) {
                newColor = Color.argb(
                    temp.get(x, kernelRadius).alpha,
                    (sumRed * scaleFactor).roundToInt(),
                    (sumGreen * scaleFactor).roundToInt(),
                    (sumBlue * scaleFactor).roundToInt()
                )
                out.set(x, kernelRadius, newColor)
            }

            // set the other pixels in the column, tracking the local sum
            for (y in kernelRadius+1..<height-kernelRadius) {
                color1 = temp.get(x, y-kernelRadius-1) // pixel that left the window
                color2 = temp.get(x, y+kernelRadius) // pixel that entered the window
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
                    out.set(x, y, newColor)
                }
            }
        }

        return out
    }

    // returns whether the given point is contained in any of the face bounding boxes
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


