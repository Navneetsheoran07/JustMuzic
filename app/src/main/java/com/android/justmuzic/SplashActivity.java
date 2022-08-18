package com.android.justmuzic;

import android.Manifest;
import android.os.Bundle;

public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermissionAndCall(Manifest.permission.READ_EXTERNAL_STORAGE, true, () -> {
            moveIntent(MainActivity.class, true);
            return null;
        });
    }
}