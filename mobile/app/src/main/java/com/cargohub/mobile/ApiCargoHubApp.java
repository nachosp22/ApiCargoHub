package com.cargohub.mobile;

import android.app.Application;

import com.cargohub.mobile.session.SessionManager;

public class ApiCargoHubApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SessionManager.init(this);
    }
}
