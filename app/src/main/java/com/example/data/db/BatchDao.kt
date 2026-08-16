package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchDao {
  @Query("SELECT * FROM batches ORDER BY name ASC")
  fun getAllBatches(): Flow<List<BatchEntity>>

  @Query("SELECT * FROM batches WHERE id = :id LIMIT 1")
  fun getBatchById(id: String): Flow<BatchEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBatch(batch: BatchEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBatches(batches: List<BatchEntity>)

  @Update
  suspend fun updateBatch(batch: BatchEntity)

  @Query("DELETE FROM batches WHERE id = :id")
  suspend fun deleteBatchById(id: String)

  @Query("DELETE FROM batches")
  suspend fun deleteAllBatches()

  @Query("SELECT COUNT(*) FROM batches")
  suspend fun getCount(): Int
}
