package com.malicankaya.pinnedplaces.database

import androidx.room.*
import com.malicankaya.pinnedplaces.models.PlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Insert
    suspend fun insert(placeEntity: PlaceEntity)
    @Update
    suspend fun update(placeEntity: PlaceEntity)
    @Delete
    suspend fun delete(placeEntity: PlaceEntity)

    @Query("select * from pinnedplacestable")
    fun getAllPlaces():Flow<List<PlaceEntity>>
    @Query("select * from pinnedplacestable where id=:id")
    fun getPlaceById(id: Int): Flow<PlaceEntity>
}