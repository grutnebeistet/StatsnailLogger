package com.roberts.adrian.statsnaillogger.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import static com.roberts.adrian.statsnaillogger.data.LogContract.TABLE_LOGS;

/**
 * Created by Adrian on 18/08/2017.
 */

public class LogContentProvider extends ContentProvider {
    static String TAG = LogContentProvider.class.getSimpleName();
    private SqlDbHelper mDbHelper;
    static final int HARVEST_LOG = 100;
    static final int HARVEST_LOG_ID = 101;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = LogContract.CONTENT_AUTHORITY;
        matcher.addURI(authority, LogContract.PATH_LOG, HARVEST_LOG);
        matcher.addURI(authority, LogContract.PATH_LOG, HARVEST_LOG_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new SqlDbHelper(getContext());
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor cursor;

        cursor = mDbHelper.getReadableDatabase().query(
                TABLE_LOGS, projection, selection, selectionArgs, null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        Log.i(TAG, "Delete");
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int rowsDel;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case HARVEST_LOG:
                rowsDel = db.delete(TABLE_LOGS, selection, selectionArgs);
                Log.i(TAG, "deleted");
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsDel != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDel;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = buildUriMatcher().match(uri);
        switch (match) {
            case HARVEST_LOG:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(LogContract.TABLE_LOGS, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long newRowId;
        newRowId = db.insert(TABLE_LOGS, null, values);
        //Log.i(TAG, "inserted : " + values.get(COLUMN_HARVEST_USER));
        if (newRowId == -1) {
            Log.e(TAG, "insertion failed for " + uri);
            return null;
        }
        // Return Uri for newly added data
        return ContentUris.withAppendedId(uri, newRowId);
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        if (values.size() == 0) return 0;

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int rowsUpdated;

        //rowsUpdated = db.up
        return 0;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }
}
