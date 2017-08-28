package com.roberts.adrian.statsnaillogger.utils;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.roberts.adrian.statsnaillogger.R;
import com.roberts.adrian.statsnaillogger.activities.MainActivity;
import com.roberts.adrian.statsnaillogger.data.LogContract;
import com.roberts.adrian.statsnaillogger.data.SqlDbHelper;

import java.io.IOException;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;

import static android.provider.CalendarContract.CalendarCache.URI;
import static com.roberts.adrian.statsnaillogger.activities.MainActivity.REQUEST_AUTHORIZATION;
import static com.roberts.adrian.statsnaillogger.data.LogContract.COLUMN_HARVEST_DATE;
import static com.roberts.adrian.statsnaillogger.data.LogContract.COLUMN_HARVEST_GRADED;
import static com.roberts.adrian.statsnaillogger.data.LogContract.COLUMN_HARVEST_ID;
import static com.roberts.adrian.statsnaillogger.data.LogContract.COLUMN_HARVEST_USER;
import static com.roberts.adrian.statsnaillogger.data.LogContract.CONTENT_LOG_ITEM_TYPE;
import static com.roberts.adrian.statsnaillogger.data.LogContract.CONTENT_URI_HARVEST_LOG;
import static com.roberts.adrian.statsnaillogger.data.LogContract.TABLE_LOGS;

/**
 * Created by Adrian on 13/08/2017.
 */

public class Utilities {
    private static final String TAG = Utilities.class.getSimpleName();

    private static FusedLocationProviderClient mFusedLocationClient;
    private static final String LOG_TAG = Utilities.class.getSimpleName();
    private static double mLongitude;
    private static double mLatitude;

    private static Location mLocatio;

    public static void retrieveLastLocation(Activity context) {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        Log.i(TAG, "getting location....");

        mFusedLocationClient.getLastLocation().addOnSuccessListener(context, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location
                mLocatio = location;
                if (location != null) {
                    // ...
                }
            }
        });
    }

    public static String getLocationUrl() {
        if (mLocatio != null) {
            mLongitude = mLocatio.getLongitude();
            mLatitude = mLocatio.getLatitude();
            String latLonBase = "https://www.google.com/maps/search/?api=1&query=" + mLatitude + "," + mLongitude;
            // TODO finn ut om mLoc kan være null/0.0 og returner evt 0.0

            return "=HYPERLINK(\"" + latLonBase + "\", \"" + mLatitude + "," + mLongitude + "\")";
        } else return "Location not available";

        // Log.i(LOG_TAG, "etter SUcc: " + mLatitude);


    }

    //TODO egen readsheet, egen DBshit
    @AfterPermissionGranted(REQUEST_AUTHORIZATION)
    public static ValueRange readShit(Activity context, com.google.api.services.sheets.v4.Sheets service) {
        Log.i(TAG, "readshit");
        ValueRange result = null;
        String spreadsheetId = context.getString(R.string.spreadsheet_id);
        String range = context.getString(R.string.spreadsheet_read_range);
        // Log.i(TAG, "name spread: " + spreadsheetId);
        try {
            Log.i(TAG, "service: " + service.toString());
            result = service.spreadsheets().values().get(spreadsheetId, range).execute();
            updateDb(context,result);

        } catch (UserRecoverableAuthIOException userRecoverableException) {
            context.startActivityForResult(
                    userRecoverableException.getIntent(), REQUEST_AUTHORIZATION); // Requests permission again (why not work before?)
            Log.e(TAG, "cause: " + userRecoverableException.getCause());
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void updateDbSingle(Context context, ValueRange newValue, int rowNum){

        ContentValues values = new ContentValues();

        String name_grader = newValue.getValues().get(0).get(4).toString();
        values.put(COLUMN_HARVEST_GRADED, name_grader);

        Uri logUri = ContentUris.withAppendedId(CONTENT_URI_HARVEST_LOG,rowNum);
        Log.i(TAG, "values: " + name_grader);
        context.getContentResolver().update(logUri,values,null,null);
        context.getContentResolver().notifyChange(logUri, null);

    }
    public static void updateDb(Context context, ValueRange result) {
        int numRows = result.getValues() != null ? result.getValues().size() - 1 : 0;  // +1 because first row consists of labels

        Cursor cursor;
        SqlDbHelper dbHelper = new SqlDbHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SQLiteDatabase dbw = dbHelper.getWritableDatabase();

        cursor = db.query(LogContract.TABLE_LOGS, null, null, null, null, null, null);
        // Log.i(TAG, "Cursor Count : " + cursor.getCount());

        if (cursor.getCount() > 0) {
            //db.delete(LogContract.TABLE_LOGS, null, null); // TODO bedre enn dette -nå slettes hver gang
            Log.i(TAG, "cursor > 0 - delete Table");
            dbw.delete(TABLE_LOGS, null, null);
            cursor.close();
        }


        List<List<Object>> logs = result.getValues(); // TODO flytte DB stuff til egen metode/ kun lagre nye innføringer?
        if (!result.isEmpty() && numRows >= 1) {
            for (int i = 1; i < logs.size(); i++) {
                List<Object> lb = logs.get(i);
                String harvestNo = lb.get(0).toString();
                String date = lb.get(1).toString();
                String name = lb.get(5).toString();
                String name_grader = lb.size() > 6 ? lb.get(10).toString() : null;


                ContentValues values = new ContentValues();
                values.put(COLUMN_HARVEST_ID, Integer.valueOf(harvestNo));
                values.put(COLUMN_HARVEST_DATE, date);
                values.put(COLUMN_HARVEST_USER, name);
                if (lb.size() > 6) {
                    // Log.i(TAG, "Been graded " + harvestNo);
                    values.put(COLUMN_HARVEST_GRADED, name_grader);
                } else {
                    // values.put(COLUMN_HARVEST_GRADED, null);
                    // Log.i(TAG, "Not graded " + harvestNo);
                } // TODO bulkinsert

                context.getContentResolver().insert(CONTENT_URI_HARVEST_LOG, values);
                //   Log.i(TAG, "lb: " + lb + " lb.size: " +lb.size() + "\nHarvNo " + harvestNo + " dato: " + date + " name: " + name);
            }
        }
    }
}
