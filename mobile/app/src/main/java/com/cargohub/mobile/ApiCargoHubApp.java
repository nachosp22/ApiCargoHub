package com.cargohub.mobile;

import android.app.Application;

import com.cargohub.mobile.session.SessionManager;

import org.osmdroid.config.Configuration;

public class ApiCargoHubApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SessionManager.init(this);

        // OSMDroid configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());
    }
}
