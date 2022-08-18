package com.android.justmuzic;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.Callable;

public class BaseActivity extends AppCompatActivity {

    public ProgressDialog progressDialog;
    Callable<Void> permCallback;
    boolean shouldRetry;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void showProgressDialog() {
        dismissProgressDialog();
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Please Wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    public void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    public void moveIntent(Class<?> clas, boolean isFinish) {
        Intent intent = new Intent(this, clas);
        startActivity(intent);
        if (isFinish)
            finish();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            getCurrentFocus().clearFocus();
        }
        return super.dispatchTouchEvent(motionEvent);
    }


    public void checkPermissionAndCall(String permission, boolean shouldRetry, Callable<Void> permCallback) {
        this.shouldRetry = shouldRetry;
        try {
            this.permCallback = null;
            if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
                permCallback.call();
            else {
                this.permCallback = permCallback;
                requestPermissions(new String[]{permission}, 12345);
            }
        } catch (Exception w) {
            showToast(w.getLocalizedMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            checkPermissionAndCall(permissions[0], shouldRetry, permCallback);
        else {
            if (!shouldShowRequestPermissionRationale(permissions[0])) {
                if (shouldRetry) {
                    Snackbar.make(findViewById(android.R.id.content), permissions[0].substring(19) + " permission required", Snackbar.LENGTH_INDEFINITE).setAction("Retry", view -> {
                        checkPermissionAndCall(permissions[0], shouldRetry, permCallback);
                    }).show();
                }
                showToast(permissions[0].substring(19) + " permission required");
                goToSettings();
            } else if (shouldRetry)
                Snackbar.make(findViewById(android.R.id.content), permissions[0].substring(19) + " permission required", Snackbar.LENGTH_INDEFINITE).setAction("Allow", view -> {
                    checkPermissionAndCall(permissions[0], shouldRetry, permCallback);
                }).show();
            else
                showToast(permissions[0].substring(19) + " permission required");
        }
    }


    public void goToSettings() {
        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(myAppSettings);
    }

}
