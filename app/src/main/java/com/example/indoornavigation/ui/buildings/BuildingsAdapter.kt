package com.example.indoornavigation.ui.buildings

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.indoornavigation.R
import com.example.indoornavigation.data.model.Building
import com.google.android.material.button.MaterialButton

class BuildingsAdapter(
    private val items: List<Building>,
    private val favoriteIds: Set<Int>,
    private val isLoggedIn: Boolean,
    private val onClick: (Building) -> Unit,
    private val onFavoriteToggle: (Building) -> Unit
) : RecyclerView.Adapter<BuildingsAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvBuildingName)
        val tvAddress: TextView = view.findViewById(R.id.tvBuildingAddress)
        val btnFavorite: MaterialButton = view.findViewById(R.id.btnFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_building, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvAddress.text = item.address
        holder.itemView.setOnClickListener { onClick(item) }

        
        if (isLoggedIn) {
            holder.btnFavorite.visibility = View.VISIBLE
            val isFav = favoriteIds.contains(item.id)
            holder.btnFavorite.setIconResource(
                if (isFav) R.drawable.ic_favorite_filled
                else R.drawable.ic_favorite_outline
            )

            val color = if (isFav) {
                Color.parseColor("#E53935") 
            } else {
                val tv = TypedValue()
                holder.itemView.context.theme.resolveAttribute(
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                    tv, true
                )
                tv.data
            }
            holder.btnFavorite.iconTint = ColorStateList.valueOf(color)
            holder.btnFavorite.setOnClickListener { onFavoriteToggle(item) }
        } else {
            holder.btnFavorite.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size
}