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
    val steps: List<Step>,
    private val onItemClick: ((Step) -> Unit)? = null
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
        
        val prefix = when (step.type) {
            "START"      -> "○ "
            "STRAIGHT"   -> "↑ "
            "TURN_LEFT"  -> "← "
            "TURN_RIGHT" -> "→ "
            "FLOOR"      -> "⇅ "
            "ARRIVE"     -> "● "
            else         -> "• "
        }
        
        holder.tvMessage.text = "$prefix${step.text}"

        // Hide the ugly Gingerbread icon frame to allow the text to occupy the full width of the card
        val bg = holder.ivIcon.parent as? View
        bg?.visibility = View.GONE

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(step)
        }
    }

    override fun getItemCount() = steps.size
}