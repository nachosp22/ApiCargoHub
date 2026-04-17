package com.cargohub.mobile.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.cargohub.mobile.data.local.entity.PendingOperationEntity;

import java.util.List;

@Dao
public interface PendingOperationDao {

    @Insert
    long insert(PendingOperationEntity operation);

    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    List<PendingOperationEntity> getAll();

    @Query("SELECT COUNT(*) FROM pending_operations")
    int count();

    @Query("DELETE FROM pending_operations WHERE id = :id")
    void deleteById(long id);

    @Query("UPDATE pending_operations SET retryCount = retryCount + 1, lastError = :error WHERE id = :id")
    void markRetry(long id, String error);

    @Query("DELETE FROM pending_operations")
    void deleteAll();
}
