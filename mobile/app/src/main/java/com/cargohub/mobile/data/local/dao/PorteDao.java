package com.cargohub.mobile.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.cargohub.mobile.data.local.entity.PorteEntity;

import java.util.List;

@Dao
public interface PorteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PorteEntity> portes);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PorteEntity porte);

    @Query("SELECT * FROM portes WHERE conductorId = :conductorId AND isOffer = 0")
    List<PorteEntity> getAssignedTrips(long conductorId);

    @Query("SELECT * FROM portes WHERE conductorId = :conductorId AND isOffer = 1")
    List<PorteEntity> getOffers(long conductorId);

    @Query("SELECT * FROM portes WHERE id = :porteId")
    PorteEntity getById(long porteId);

    @Query("DELETE FROM portes WHERE conductorId = :conductorId AND isOffer = :isOffer")
    void deleteByConductorAndType(long conductorId, boolean isOffer);

    @Query("DELETE FROM portes")
    void deleteAll();
}
