package com.cargohub.mobile.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.cargohub.mobile.data.local.dao.AgendaBloqueoDao;
import com.cargohub.mobile.data.local.dao.ConductorDao;
import com.cargohub.mobile.data.local.dao.PendingOperationDao;
import com.cargohub.mobile.data.local.dao.PorteDao;
import com.cargohub.mobile.data.local.dao.VehiculoDao;
import com.cargohub.mobile.data.local.entity.AgendaBloqueoEntity;
import com.cargohub.mobile.data.local.entity.ConductorEntity;
import com.cargohub.mobile.data.local.entity.PendingOperationEntity;
import com.cargohub.mobile.data.local.entity.PorteEntity;
import com.cargohub.mobile.data.local.entity.VehiculoEntity;

@Database(
        entities = {
                PorteEntity.class,
                ConductorEntity.class,
                VehiculoEntity.class,
                AgendaBloqueoEntity.class,
                PendingOperationEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract PorteDao porteDao();
    public abstract ConductorDao conductorDao();
    public abstract VehiculoDao vehiculoDao();
    public abstract AgendaBloqueoDao agendaBloqueoDao();
    public abstract PendingOperationDao pendingOperationDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "cargohub_cache.db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
