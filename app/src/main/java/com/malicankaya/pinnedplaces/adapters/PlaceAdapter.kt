package com.malicankaya.pinnedplaces.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.malicankaya.pinnedplaces.databinding.ItemPlaceBinding
import com.malicankaya.pinnedplaces.models.PlaceEntity

class PlaceAdapter(
    private val placeList: ArrayList<PlaceEntity>,
    private val openDetails: (id: Int) -> Unit
) : RecyclerView.Adapter<PlaceAdapter.ViewHolder>() {

    class ViewHolder(val itemBinding: ItemPlaceBinding) :
        RecyclerView.ViewHolder(itemBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemPlaceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemBinding.sivPlaceImage.setImageURI(Uri.parse(placeList[position].image))
        holder.itemBinding.tvTitle.text = placeList[position].title
        holder.itemBinding.tvDescription.text = placeList[position].description

        holder.itemBinding.root.setOnClickListener {
            openDetails.invoke(placeList[position].id)
        }
    }

    override fun getItemCount(): Int {
        return placeList.size
    }
}