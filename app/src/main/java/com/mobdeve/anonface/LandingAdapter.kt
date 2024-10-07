package com.mobdeve.anonface

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter

class LandingAdapter(private val landing : ArrayList<LandingDetails>) : Adapter<LandingViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LandingViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.recycler_template, parent, false)

        return LandingViewHolder(view)
    }

    override fun getItemCount(): Int {
        return landing.size
    }

    override fun onBindViewHolder(holder: LandingViewHolder, position: Int) {
        holder.bindData(landing[position])
    }

}