package com.mobdeve.anonface

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class PageIndicator(private val activity: Activity) : RecyclerView.OnScrollListener() {
    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)

        val position = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(0))
        var indicator1 = activity.findViewById<ImageView>(R.id.indicator1)
        var indicator2 = activity.findViewById<ImageView>(R.id.indicator2)
        var indicator3 = activity.findViewById<ImageView>(R.id.indicator3)
        var buttonTakePhoto = activity.findViewById<Button>(R.id.startPhotoCaptureActivityBtn)
        var buttonSelectGallery = activity.findViewById<Button>(R.id.selectFromGalleryBtn)

        var indicators : ArrayList<ImageView> = arrayListOf(indicator1, indicator2, indicator3)
        indicators.forEach {
            it.setImageResource(R.drawable.indicator_deselected)
        }
        indicators[position].setImageResource(R.drawable.indicator_selected)

        if (position < 2) {
            buttonTakePhoto.visibility = View.GONE
            buttonSelectGallery.visibility = View.GONE
        } else {
            buttonTakePhoto.visibility = View.VISIBLE
            buttonSelectGallery.visibility = View.VISIBLE
        }

        //button
        //var buttonText = arrayListOf(R.string.landing_button1,R.string.landing_button1,R.string.landing_button2)
        //button.text = activity.resources.getString(buttonText[position])

    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
    }
}