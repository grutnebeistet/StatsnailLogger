package com.roberts.adrian.statsnaillogger.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.roberts.adrian.statsnaillogger.R;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.roberts.adrian.statsnaillogger.activities.MainActivity.REQUEST_PERMISSION_ACCESS_FINE_LOCATION;

public class ChooserActivity extends AppCompatActivity
        implements View.OnClickListener {
    private static final String TAG = ChooserActivity.class.getSimpleName();
    private Bundle mInfo;
    private SharedPreferences mSharedPreferences;

    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chooser);

        Button grading = (Button) findViewById(R.id.open_grading_button);
        Button weighing = (Button) findViewById(R.id.open_weighing_button);
      //  mDrawerLayout =(DrawerLayout)findViewById(R.id.drawer_layout);
        //mDrawerList = (ListView)findViewById(R.id.left_drawer);

        // Set the adapter for the list view
       /* mDrawerList.setAdapter(new ArrayAdapter<>(this,
                R.layout.drawer_list_item, "LogoutTest"));*/
        // Set the list's click listener
      //  mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,R.string.drawer_open,R.string.drawer_close){

        //};

        handleLocationPermission();


        grading.setOnClickListener(this);
        weighing.setOnClickListener(this);

        mInfo = getIntent().getExtras();

        mSharedPreferences = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

    }

    @AfterPermissionGranted(REQUEST_PERMISSION_ACCESS_FINE_LOCATION)
    private void handleLocationPermission() {
        if (!EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            EasyPermissions.requestPermissions(this,
                    "turn on location forr faen",
                    REQUEST_PERMISSION_ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }


    @Override
    public void onClick(View v) {
        Intent loggingActivity = new Intent(this, MainActivity.class);
        loggingActivity.putExtras(mInfo);

        SharedPreferences.Editor prefEditor = mSharedPreferences.edit();

        switch (v.getId()) {
            case (R.id.open_weighing_button):
                //prefEditor.putString(getString(R.string.snail_logging_mode), getString(R.string.logging_mode_weighing));
                prefEditor.putBoolean(getString(R.string.logging_mode_weighing), true);
                prefEditor.putBoolean(getString(R.string.logging_mode_grading), false);
//                loggingActivity.putExtra(getString(R.string.snail_logging_mode), getString(R.string.logging_mode_weighing));
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
