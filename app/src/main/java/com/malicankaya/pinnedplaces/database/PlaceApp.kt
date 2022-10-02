package com.malicankaya.pinnedplaces.database

import android.app.Application

class PlaceApp:Application() {
    val db by lazy {
        PlaceDatabase.getInstance(this)
    }
}