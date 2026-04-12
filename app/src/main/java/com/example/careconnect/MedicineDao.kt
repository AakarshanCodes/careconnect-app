package com.example.careconnect

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface MedicineDao {

    @Insert
    suspend fun insert(medicine: Medicine)

    @Query("SELECT * FROM medicine_table")
    fun getAllMedicines(): kotlinx.coroutines.flow.Flow<List<Medicine>>

    @Delete
    suspend fun delete(medicine: Medicine)
}
