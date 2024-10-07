package com.mobdeve.anonface

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
        var button : Button = findViewById(R.id.startPhotoCaptureActivityBtn)
        button.setOnClickListener {
            currPos = (landingRecycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            landingRecycler.smoothScrollToPosition(currPos+1)
        }

        if (currPos < 2) {
            button.visibility = View.GONE
        } else {
            button.visibility = View.VISIBLE
        }

        if (!permissionsGranted()) {
            activityResultLauncher.launch(REQUIRED_PERMISSIONS) // get permissions
        }

        val startPhotoCaptureActivityBtn: Button = findViewById(R.id.startPhotoCaptureActivityBtn)
        startPhotoCaptureActivityBtn.setOnClickListener {
            val intent = Intent(baseContext, PhotoCaptureActivity::class.java)
            startActivity(intent)
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