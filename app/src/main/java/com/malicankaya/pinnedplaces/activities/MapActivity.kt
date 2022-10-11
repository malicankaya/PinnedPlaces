package com.malicankaya.pinnedplaces.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.malicankaya.pinnedplaces.R
import com.malicankaya.pinnedplaces.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private var binding: ActivityMapBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setSupportActionBar(binding?.toolbarMap)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding?.toolbarMap?.setNavigationOnClickListener {
            onBackPressed()
        }

        if(intent.hasExtra("latlong")){
            val supportMapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map)
                    as SupportMapFragment
            supportMapFragment.getMapAsync(this)
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        val latlong = intent.extras?.getDoubleArray("latlong")
        val locationTitle = intent.extras?.getString("title")
        val position = LatLng(latlong!![0], latlong!![1])
        val latlongZoom = CameraUpdateFactory.newLatLngZoom(position,10f)
        googleMap?.addMarker(MarkerOptions().position(position).title(locationTitle))
        googleMap?.animateCamera(latlongZoom)
        Log.e("Lokasyon",""+latlong)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding= null
    }


}