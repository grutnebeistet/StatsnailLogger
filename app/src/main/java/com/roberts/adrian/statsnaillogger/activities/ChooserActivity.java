package com.roberts.adrian.statsnaillogger.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.roberts.adrian.statsnaillogger.R;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class ChooserActivity extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks,
        View.OnClickListener {
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    static final int REQUEST_PERMISSION_ACCESS_FINE_LOCATION = 1004;
    private static final String TAG = ChooserActivity.class.getSimpleName();
    private Bundle mInfo;
    private SharedPreferences mSharedPreferences;

    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    DrawerLayout mDrawerLayout;

    private Button mGrading;
    private Button mWeighing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chooser);

        mGrading = (Button) findViewById(R.id.open_grading_button);
        mWeighing = (Button) findViewById(R.id.open_weighing_button);

        handleLocationPermission();


        mGrading.setOnClickListener(this);
        mWeighing.setOnClickListener(this);
        mGrading.setEnabled(false);
        mWeighing.setEnabled(false);

        mInfo = getIntent().getExtras();

        mSharedPreferences = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            // Do something after user returned from app settings screen, like showing a Toast.
            Toast.makeText(this, R.string.returned_from_app_settings_to_activity, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_ACCESS_FINE_LOCATION)
    public void handleLocationPermission() {
        Log.i(TAG, "handleLocPerm");
        if (EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

            getContactPermission();

        } else {
            EasyPermissions.requestPermissions(this,
                    "turn on location forr faen",
                    REQUEST_PERMISSION_ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }


    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    public void getContactPermission() {

        Log.i(TAG, "getContactPerm");
        if (EasyPermissions.hasPermissions(this,
                Manifest.permission.GET_ACCOUNTS)) {
            mGrading.setEnabled(true);
            mWeighing.setEnabled(true);


        } else {
            Log.i(TAG, "getContactPerm - requestPerm");
            EasyPermissions.requestPermissions(this,
                    "need contact perm",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    @Override
    public void onClick(View v) {
        Intent loggingActivity = new Intent(this, MainActivity.class);
        loggingActivity.putExtras(mInfo);

        SharedPreferences.Editor prefEditor = mSharedPreferences.edit();

        switch (v.getId()) {
            case (R.id.open_weighing_button):
                prefEditor.putBoolean(getString(R.string.logging_mode_weighing), true);
                prefEditor.putBoolean(getString(R.string.logging_mode_grading), false);
                break;

            case R.id.open_grading_button:
                prefEditor.putBoolean(getString(R.string.logging_mode_grading), true);
                prefEditor.putBoolean(getString(R.string.logging_mode_weighing), false);
                // prefEditor.putString(getString(R.string.snail_logging_mode), getString(R.string.logging_mode_grading));
                //  loggingActivity.putExtra(getString(R.string.snail_logging_mode), getString(R.string.logging_mode_grading));
                break;
        }
        prefEditor.commit();
        startActivity(loggingActivity);

    }
}
