package com.example.indoornavigation.ui.map

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.indoornavigation.R
import com.example.indoornavigation.data.model.*

class StepsAdapter(
    private val steps: List<Step>
) : RecyclerView.Adapter<StepsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView  = view.findViewById(R.id.tvStepMessage)
        val ivIcon:    ImageView = view.findViewById(R.id.ivStepIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_step, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val step = steps[position]
        holder.tvMessage.text = step.text

        val (iconRes, bgColor) = when (step.type) {
            "START"      -> Pair(android.R.drawable.ic_menu_mylocation,  "#DBEAFE")
            "STRAIGHT"   -> Pair(android.R.drawable.ic_menu_directions,  "#DCFCE7")
            "TURN_LEFT"  -> Pair(android.R.drawable.ic_media_rew,         "#FEF9C3")
            "TURN_RIGHT" -> Pair(android.R.drawable.ic_media_ff,          "#FEF9C3")
            "FLOOR"      -> Pair(android.R.drawable.ic_menu_upload,       "#EDE9FE")
            "ARRIVE"     -> Pair(android.R.drawable.ic_menu_myplaces,     "#FCE7F3")
            else         -> Pair(android.R.drawable.ic_menu_info_details, "#F3F4F6")
        }

        holder.ivIcon.setImageResource(iconRes)

        
        val bg = holder.ivIcon.parent as? View
        bg?.background?.setTint(Color.parseColor(bgColor))

        
        holder.itemView.alpha = if (step.type == "ARRIVE") 1f else 1f
    }

    override fun getItemCount() = steps.size
}