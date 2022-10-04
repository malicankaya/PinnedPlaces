package com.malicankaya.pinnedplaces.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "pinnedplacestable")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val image: String,
    val description: String,
    val date: String,
    val location: String,
    val latitude: Double,
    val longitude: Double
)