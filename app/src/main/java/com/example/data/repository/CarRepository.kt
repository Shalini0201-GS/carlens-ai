package com.example.data.repository

import com.example.data.local.CarDao
import com.example.data.local.ScannedCarEntity
import kotlinx.coroutines.flow.Flow

class CarRepository(private val carDao: CarDao) {
    val allCars: Flow<List<ScannedCarEntity>> = carDao.getAllCars()
    val carsCount: Flow<Int> = carDao.getCarsCount()

    fun searchCars(query: String): Flow<List<ScannedCarEntity>> {
        return carDao.searchCars(query)
    }

    suspend fun getCarById(id: Int): ScannedCarEntity? {
        return carDao.getCarById(id)
    }

    suspend fun saveCar(car: ScannedCarEntity): Long {
        return carDao.insertCar(car)
    }

    suspend fun deleteCar(id: Int) {
        carDao.deleteCarById(id)
    }

    suspend fun clearGarage() {
        carDao.deleteAllCars()
    }
}
