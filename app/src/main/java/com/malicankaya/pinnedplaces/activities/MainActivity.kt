package com.malicankaya.pinnedplaces.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.malicankaya.pinnedplaces.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.fabAddPlace?.setOnClickListener {
            val intent = Intent(this@MainActivity, AddPlaceActivity::class.java)
            startActivity(intent)

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}