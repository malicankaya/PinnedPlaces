package com.malicankaya.pinnedplaces.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.malicankaya.pinnedplaces.PlaceAdapter
import com.malicankaya.pinnedplaces.database.PlaceApp
import com.malicankaya.pinnedplaces.database.PlaceDao
import com.malicankaya.pinnedplaces.databinding.ActivityMainBinding
import com.malicankaya.pinnedplaces.models.PlaceEntity
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

    }

    private fun setRecyclerView() {
        if(placesList == null){
            binding?.rvPlacesList?.visibility = View.INVISIBLE
            binding?.tvNoRecordsAvailable?.visibility = View.VISIBLE
        }else{
            binding?.rvPlacesList?.visibility = View.VISIBLE
            binding?.tvNoRecordsAvailable?.visibility = View.INVISIBLE


            binding?.rvPlacesList?.adapter = PlaceAdapter(placesList!!)
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

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}