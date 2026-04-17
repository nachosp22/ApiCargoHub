package com.cargohub.mobile.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.cargohub.mobile.data.local.entity.ConductorEntity;

@Dao
public interface ConductorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ConductorEntity conductor);

    @Query("SELECT * FROM conductores WHERE id = :conductorId")
    ConductorEntity getById(long conductorId);

    @Query("DELETE FROM conductores")
    void deleteAll();
}
