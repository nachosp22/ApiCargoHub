package com.cargohub.mobile.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.cargohub.mobile.data.local.entity.VehiculoEntity;

import java.util.List;

@Dao
public interface VehiculoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<VehiculoEntity> vehiculos);

    @Query("SELECT * FROM vehiculos WHERE conductorId = :conductorId")
    List<VehiculoEntity> getByConductor(long conductorId);

    @Query("DELETE FROM vehiculos WHERE conductorId = :conductorId")
    void deleteByConductor(long conductorId);

    @Query("DELETE FROM vehiculos")
    void deleteAll();
}
