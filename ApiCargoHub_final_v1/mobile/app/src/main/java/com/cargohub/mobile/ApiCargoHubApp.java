package com.cargohub.mobile;

import android.app.Application;

import com.cargohub.mobile.data.local.AppDatabase;
import com.cargohub.mobile.network.ConnectivityObserver;
import com.cargohub.mobile.session.SessionManager;

import org.osmdroid.config.Configuration;

public class ApiCargoHubApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SessionManager.init(this);

        // Initialize offline infrastructure
        AppDatabase.getInstance(this);
        ConnectivityObserver.getInstance(this).start();

        // OSMDroid configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());
    }
}
