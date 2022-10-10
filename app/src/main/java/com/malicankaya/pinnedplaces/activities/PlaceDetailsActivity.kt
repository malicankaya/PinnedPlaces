package com.malicankaya.pinnedplaces.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.malicankaya.pinnedplaces.R
import com.malicankaya.pinnedplaces.database.PlaceApp
import com.malicankaya.pinnedplaces.database.PlaceDao
import com.malicankaya.pinnedplaces.databinding.ActivityPlaceDetailsBinding
import com.malicankaya.pinnedplaces.models.PlaceEntity
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch

class PlaceDetailsActivity : AppCompatActivity() {
    private var binding: ActivityPlaceDetailsBinding? = null
    private var placeID: Int = 0
    private var placeDao: PlaceDao? = null
    private var place: PlaceEntity? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaceDetailsBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.toolbarPlaceDetails)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding?.toolbarPlaceDetails?.setNavigationOnClickListener {
            onBackPressed()
        }

        placeID = intent.getIntExtra("placeID", 0)
        placeDao = (application as PlaceApp).db.placeDao()

        setUpDetails()

        binding?.btnViewOnMap?.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("latlong", arrayOf(place?.latitude,place?.longitude))
            startActivity(intent)
        }
    }

    private fun setUpDetails() {
        lifecycleScope.launch {
            placeDao?.getPlaceById(placeID)?.collect {
                place = it
                runOnUiThread {
                    binding?.toolbarPlaceDetails?.title = it.title
                    binding?.ivPlaceImage?.setImageURI(Uri.parse(it.image))
                    binding?.tvDescription?.text = it.description
                    binding?.tvLocation?.text = it.location
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}