package com.cargohub.mobile;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

public final class LoginNavigator {

    private LoginNavigator() {
    }

    public static void openLoginAndFinish(@NonNull Activity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
