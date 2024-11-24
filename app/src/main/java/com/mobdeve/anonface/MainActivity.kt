package com.mobdeve.anonface

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private var galleryUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get image from gallery
        val myActivityResultLauncher = registerForActivityResult(
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

        //Recycler image slider
        var landingSlider : ArrayList<LandingDetails> = arrayListOf(
            LandingDetails(R.drawable.landing_1,R.string.landing_text1),
            LandingDetails(R.drawable.landing_2,R.string.landing_text2),
            LandingDetails(R.drawable.landing_3,R.string.landing_text3)
        )

        var landingRecycler : RecyclerView = findViewById(R.id.landingRecycler)
        landingRecycler.adapter = LandingAdapter(landingSlider)
        landingRecycler.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(landingRecycler)
        landingRecycler.addOnScrollListener(PageIndicator(this))

        var currPos : Int = 0

        // Buttons
        val startPhotoCaptureActivityBtn : Button = findViewById(R.id.startPhotoCaptureActivityBtn)
        startPhotoCaptureActivityBtn.setOnClickListener {
            currPos = (landingRecycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            landingRecycler.smoothScrollToPosition(currPos+1)
            val intent = Intent(baseContext, PhotoCaptureActivity::class.java)
            startActivity(intent)
        }

        val selectFromGalleryBtn : Button = findViewById(R.id.selectFromGalleryBtn)
        selectFromGalleryBtn.setOnClickListener {
            currPos = (landingRecycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            landingRecycler.smoothScrollToPosition(currPos+1)
            val intent: Intent = Intent().apply {
                type = "image/*"
                action = Intent.ACTION_OPEN_DOCUMENT
            }
            myActivityResultLauncher.launch(Intent.createChooser(intent, "Select Picture"))
        }

        if (currPos < 2) {
            startPhotoCaptureActivityBtn.visibility = View.GONE
            selectFromGalleryBtn.visibility = View.GONE
        } else {
            startPhotoCaptureActivityBtn.visibility = View.VISIBLE
            selectFromGalleryBtn.visibility = View.VISIBLE
        }

        if (!permissionsGranted()) {
            activityResultLauncher.launch(REQUIRED_PERMISSIONS) // get permissions
        }
    }

    private fun permissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
                it ->
            ContextCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        {
            permissions ->
            var permissionsGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionsGranted = false
            }
            if (!permissionsGranted)
                finish()
        }

    companion object {
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}