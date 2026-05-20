package com.example.indoornavigation.ui.buildings

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.indoornavigation.R
import com.example.indoornavigation.data.model.Floor

class FloorsAdapter(
    private val items: List<Floor>,
    private val onClick: (Floor) -> Unit
) : RecyclerView.Adapter<FloorsAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvSub: TextView = view.findViewById(R.id.tvSub)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text = com.example.indoornavigation.ui.common.LocalizationHelper.localizeName(item, holder.itemView.context)
        val isEn = holder.itemView.context.resources.configuration.locales[0].language == "en"
        holder.tvSub.text = if (isEn) "Floor ${item.id}" else "Этаж ${item.id}"
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}