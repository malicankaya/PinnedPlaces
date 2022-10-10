package com.malicankaya.pinnedplaces.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.malicankaya.pinnedplaces.R
import com.malicankaya.pinnedplaces.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity() {
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
    }

    override fun onDestroy() {
        super.onDestroy()
        binding= null
    }
}