package com.cargohub.mobile.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.cargohub.mobile.data.local.entity.AgendaBloqueoEntity;

import java.util.List;

@Dao
public interface AgendaBloqueoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AgendaBloqueoEntity> bloqueos);

    @Query("SELECT * FROM agenda_bloqueos WHERE conductorId = :conductorId")
    List<AgendaBloqueoEntity> getByConductor(long conductorId);

    @Query("DELETE FROM agenda_bloqueos WHERE conductorId = :conductorId")
    void deleteByConductor(long conductorId);

    @Query("DELETE FROM agenda_bloqueos")
    void deleteAll();
}
