package com.roberts.adrian.statsnaillogger.data;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Adrian on 18/08/2017.
 */

public class LogContract implements BaseColumns {
    public static final String CONTENT_AUTHORITY = "com.roberts.adrian.statsnaillogger";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_LOG = "harvest_log";

    public static final Uri CONTENT_URI_HARVEST_LOG = BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOG).build();

    public static final String TABLE_LOGS = "harvest_logs";

    public static final String COLUMN_HARVEST_ID = "harvest_id";
    public static final String COLUMN_HARVEST_USER = "harvest_user";
    public static final String COLUMN_HARVEST_DATE = "harvest_date";
    public static final String COLUMN_HARVEST_GRADED = "harvest_graded";

    public static final int BEEN_GRADED = 1;
    public static final int NOT_GRADED = 0;




}
