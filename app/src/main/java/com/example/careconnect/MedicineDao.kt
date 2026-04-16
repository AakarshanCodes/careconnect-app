package com.example.careconnect

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {

    @Insert
    suspend fun insert(medicine: Medicine)

    @Query("SELECT * FROM medicine_table WHERE userId = :userId")
    fun getMedicinesForUser(userId: String): Flow<List<Medicine>>

    @Delete
    suspend fun delete(medicine: Medicine)
}
