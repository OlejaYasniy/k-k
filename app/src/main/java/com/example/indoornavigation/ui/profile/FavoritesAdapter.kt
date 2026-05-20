package com.example.indoornavigation.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.indoornavigation.R
import com.example.indoornavigation.data.local.entity.FavoriteEntity
import com.google.android.material.button.MaterialButton

class FavoritesAdapter(
    private val onDelete: (FavoriteEntity) -> Unit,
    private val onItemClick: (FavoriteEntity) -> Unit
) : ListAdapter<FavoriteEntity, FavoritesAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FavoriteEntity>() {
            override fun areItemsTheSame(a: FavoriteEntity, b: FavoriteEntity) = a.id == b.id
            override fun areContentsTheSame(a: FavoriteEntity, b: FavoriteEntity) = a == b
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName:    TextView      = view.findViewById(R.id.tvFavName)
        val tvAddress: TextView      = view.findViewById(R.id.tvFavAddress)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnFavDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.tvName.text    = com.example.indoornavigation.ui.common.LocalizationHelper.localizeName(item, holder.itemView.context)
        holder.tvAddress.text = com.example.indoornavigation.ui.common.LocalizationHelper.localizeAddress(item, holder.itemView.context)
        holder.btnDelete.setOnClickListener { onDelete(item) }
        holder.itemView.setOnClickListener { onItemClick(item) }
    }
}