package com.malicankaya.pinnedplaces.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.malicankaya.pinnedplaces.adapters.PlaceAdapter
import com.malicankaya.pinnedplaces.database.PlaceApp
import com.malicankaya.pinnedplaces.database.PlaceDao
import com.malicankaya.pinnedplaces.databinding.ActivityMainBinding
import com.malicankaya.pinnedplaces.models.PlaceEntity
import com.malicankaya.pinnedplaces.utils.SwipeToDeleteCallback
import com.malicankaya.pinnedplaces.utils.SwipeToEditCallback
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private var placesList: ArrayList<PlaceEntity>? = null
    private var placeDao: PlaceDao? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.fabAddPlace?.setOnClickListener {
            val intent = Intent(this@MainActivity, AddPlaceActivity::class.java)
            startActivity(intent)
        }
        placeDao = (application as PlaceApp).db.placeDao()
        getAllPlaces()

        //swipe to edit things
        val editSwipeHandler = object: SwipeToEditCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding?.rvPlacesList?.adapter as PlaceAdapter
                adapter.notifyEditItem(this@MainActivity, viewHolder.layoutPosition)
            }
        }
        val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
        editItemTouchHelper.attachToRecyclerView(binding?.rvPlacesList)
        //swipe to delete things
        val deleteSwipeHandler = object: SwipeToDeleteCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding?.rvPlacesList?.adapter as PlaceAdapter
                val deletePlaceID = adapter.notifyDeleteItem(viewHolder.layoutPosition)
                deletePlace(deletePlaceID)
            }
        }
        val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHelper.attachToRecyclerView(binding?.rvPlacesList)
    }

    private fun deletePlace(id: Int) {
        lifecycleScope.launch {
            placeDao?.delete(PlaceEntity(id,"","","","","",0.0,0.0))
        }
    }

    private fun setRecyclerView() {
        if(placesList == null){
            binding?.rvPlacesList?.visibility = View.INVISIBLE
            binding?.tvNoRecordsAvailable?.visibility = View.VISIBLE
        }else{
            binding?.rvPlacesList?.visibility = View.VISIBLE
            binding?.tvNoRecordsAvailable?.visibility = View.INVISIBLE


            binding?.rvPlacesList?.adapter = PlaceAdapter(this,placesList!!,{
                openDetailsActivity(it)
            })
        }
    }

    private fun getAllPlaces() {
        if (placesList == null) {
            lifecycleScope.launch {
                placeDao?.getAllPlaces()?.collect {
                    placesList = ArrayList(it)
                    setRecyclerView()
                }
            }
        }
    }

    private fun openDetailsActivity(id: Int){
        val intent = Intent(this, PlaceDetailsActivity::class.java)
        intent.putExtra("placeID",id)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}