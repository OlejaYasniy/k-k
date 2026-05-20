package com.example.indoornavigation.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.indoornavigation.R
import com.example.indoornavigation.data.local.entity.SearchHistoryEntity
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onDelete: (SearchHistoryEntity) -> Unit,
    private val onItemClick: (SearchHistoryEntity) -> Unit
) : ListAdapter<SearchHistoryEntity, HistoryAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SearchHistoryEntity>() {
            override fun areItemsTheSame(a: SearchHistoryEntity, b: SearchHistoryEntity) = a.id == b.id
            override fun areContentsTheSame(a: SearchHistoryEntity, b: SearchHistoryEntity) = a == b
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvRoute:    TextView       = view.findViewById(R.id.tvHistoryRoute)
        val tvBuilding: TextView       = view.findViewById(R.id.tvHistoryBuilding)
        val tvTime:     TextView       = view.findViewById(R.id.tvHistoryTime)
        val btnDelete:  MaterialButton = view.findViewById(R.id.btnHistoryDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val fromName = com.example.indoornavigation.ui.common.LocalizationHelper.localizeHistoryFrom(item, holder.itemView.context)
        val toName = com.example.indoornavigation.ui.common.LocalizationHelper.localizeHistoryTo(item, holder.itemView.context)
        holder.tvRoute.text    = "$fromName → $toName"
        holder.tvBuilding.text = item.buildingName
        holder.tvTime.text     = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            .format(Date(item.timestamp))
        holder.btnDelete.setOnClickListener { onDelete(item) }
        holder.itemView.setOnClickListener { onItemClick(item) }
    }
}