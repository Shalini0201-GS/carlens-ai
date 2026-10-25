package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM scanned_cars ORDER BY timestamp DESC")
    fun getAllCars(): Flow<List<ScannedCarEntity>>

    @Query("SELECT * FROM scanned_cars WHERE make LIKE '%' || :query || '%' OR model LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchCars(query: String): Flow<List<ScannedCarEntity>>

    @Query("SELECT * FROM scanned_cars WHERE id = :id LIMIT 1")
    suspend fun getCarById(id: Int): ScannedCarEntity?

    @Query("SELECT COUNT(*) FROM scanned_cars")
    fun getCarsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: ScannedCarEntity): Long

    @Query("DELETE FROM scanned_cars WHERE id = :id")
    suspend fun deleteCarById(id: Int)

    @Query("DELETE FROM scanned_cars")
    suspend fun deleteAllCars()
}
