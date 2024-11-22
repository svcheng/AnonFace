package com.mobdeve.anonface

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LandingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val frame : FrameLayout = itemView.findViewById(R.id.backgroundImg)
    val flairText : TextView = itemView.findViewById(R.id.flairText)

    fun bindData(landingDetails: LandingDetails) {
        frame.background = itemView.resources.getDrawable(landingDetails.background)
        flairText.text = itemView.resources.getString(landingDetails.flairText)
    }
}