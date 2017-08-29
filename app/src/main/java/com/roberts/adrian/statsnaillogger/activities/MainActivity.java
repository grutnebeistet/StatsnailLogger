package com.roberts.adrian.statsnaillogger.activities;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.roberts.adrian.statsnaillogger.R;
import com.roberts.adrian.statsnaillogger.adapters.HarvestLogAdapter;
import com.roberts.adrian.statsnaillogger.data.LogContract;
import com.roberts.adrian.statsnaillogger.data.SqlDbHelper;
import com.roberts.adrian.statsnaillogger.utils.Utilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import pub.devrel.easypermissions.EasyPermissions;

import static com.roberts.adrian.statsnaillogger.data.LogContract.COLUMN_HARVEST_DATE;
import static com.roberts.adrian.statsnaillogger.data.LogContract.COLUMN_HARVEST_GRADED;
import static com.roberts.adrian.statsnaillogger.data.LogContract.COLUMN_HARVEST_ID;
import static com.roberts.adrian.statsnaillogger.data.LogContract.COLUMN_HARVEST_USER;
import static com.roberts.adrian.statsnaillogger.data.LogContract.CONTENT_URI_HARVEST_LOG;
import static com.roberts.adrian.statsnaillogger.data.LogContract.TABLE_LOGS;

public class MainActivity extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks,
        View.OnTouchListener,
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = MainActivity.class.getSimpleName();

    GoogleAccountCredential mCredential;
    private GoogleSignInAccount mGoogleAccount;
    private GoogleCredential mCredentiall;

    NumberPicker mNumberPicker;
    private Button mRegButton;
    private EditText mUserInputCatch;
    private EditText mEditTextSuperJumbo;
    private EditText mEditTextJumbo;
    private EditText mEditTextLarge;

    private Spinner mSpinnerHarvestNo;
    private FloatingActionButton mFab;


    private HarvestLogAdapter mLogAdapter;
    RecyclerView recyclerView;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    public static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;



    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {SheetsScopes.SPREADSHEETS, "https://www.googleapis.com/auth/plus.login"};
    private com.google.api.services.sheets.v4.Sheets mService = null;


    private boolean mWeighingMode;
    private boolean mGradingMode;
    private SharedPreferences mSharedPreferences;
    private CheckBox mCheckBox;

    private ValueRange mExistingRows;


    private static final String[] PROJECTION = {
            COLUMN_HARVEST_ID,
            COLUMN_HARVEST_DATE,
            COLUMN_HARVEST_USER,
            COLUMN_HARVEST_GRADED
    };
    public static final int INDEX_HARVEST_ID = 0;
    public static final int INDEX_HARVEST_DATE = 1;
    public static final int INDEX_HARVEST_USER = 2;
    public static final int INDEX_HARVEST_GRADED = 3;

    private static final int HARVEST_LOG_LOADER_ID = 1349;

    /**
     * Create the main activity.
     *
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSharedPreferences = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        mWeighingMode = mSharedPreferences.getBoolean(getString(R.string.logging_mode_weighing), false);
        mGradingMode = mSharedPreferences.getBoolean(getString(R.string.logging_mode_grading), false);


        // Start retrieving last known location
        if (EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION))
            Utilities.retrieveLastLocation(this);

        if (mWeighingMode) setupWeighingUi();
        else if (mGradingMode) setupGradingUi();

        // google account sent from SignInActivity/launchActivity
        mGoogleAccount = (GoogleSignInAccount) getIntent().getExtras().get("GoogleSignInAccount");

        mCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(),
                Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(mGoogleAccount.getAccount().name);

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName("Statsnail Catch Logger") // TODO R.string.
                .build();


        //getContactPermission();
        getResultsFromApi();
        Thread readSheet = new Thread(new Runnable() {
            @Override
            public void run() {
                mExistingRows = Utilities.readShit(MainActivity.this, mService);
            }
        });
        readSheet.start();


        Log.i(TAG, "name: " + mCredential.getSelectedAccountName());


        mFab = (FloatingActionButton)

                findViewById(R.id.fab_post_data);
        mFab.setEnabled(false);
        mFab.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "fab clicked");
                if (mWeighingMode) {

                    Thread postWeightsThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            postWeighingData();
                        }
                    });
                    postWeightsThread.start();
                } else {
                    Thread postGradingsThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            postGradingData();
                        }
                    });
                    postGradingsThread.start();
                }
                //TODO til seperat method - rydde
            }
        });
        //     getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        mCheckBox = (CheckBox) findViewById(R.id.confirm_checkbox);
        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()

        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // mRegButton.setEnabled(true);
                    mFab.setEnabled(true);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                    if (mWeighingMode) {
                        imm.hideSoftInputFromWindow(mUserInputCatch.getWindowToken(), 0);
                        mUserInputCatch.setEnabled(false);

                    }
                    if (mGradingMode) {
                        imm.hideSoftInputFromWindow(mEditTextSuperJumbo.getWindowToken(), 0);
                        imm.hideSoftInputFromWindow(mEditTextJumbo.getWindowToken(), 0);
                        imm.hideSoftInputFromWindow(mEditTextLarge.getWindowToken(), 0);

                        mEditTextSuperJumbo.setEnabled(false);
                        mEditTextJumbo.setEnabled(false);
                        mEditTextLarge.setEnabled(false);
                        mSpinnerHarvestNo.setEnabled(false);
                    }

                } else {
                    //mRegButton.setEnabled(false);
                    mFab.setEnabled(false);

                    if (mWeighingMode) {
                        mUserInputCatch.setEnabled(true);

                    }
                    if (mGradingMode) {
                        mEditTextSuperJumbo.setEnabled(true);
                        mEditTextJumbo.setEnabled(true);
                        mEditTextLarge.setEnabled(true);
                        mSpinnerHarvestNo.setEnabled(true);
                    }
                }
            }
        });

        if (mGradingMode) {
            getSupportLoaderManager().initLoader(HARVEST_LOG_LOADER_ID, null, this);
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.i(TAG, "onCLoader");
        return new CursorLoader(this,
                CONTENT_URI_HARVEST_LOG,
                PROJECTION,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mLogAdapter.swapCursor(data);
        Log.i(TAG, "RV count: " + recyclerView.getLayoutManager().getItemCount());
        recyclerView.scrollToPosition(recyclerView.getLayoutManager().getItemCount() - 1);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mLogAdapter.swapCursor(null);
    }

    private void setupWeighingUi() {
        setContentView(R.layout.activity_weighing);

        mUserInputCatch = (EditText) findViewById(R.id.catch_edit_text);

        mUserInputCatch.setOnTouchListener(this);
    }

    private void setupGradingUi() {
        setContentView(R.layout.activity_grading);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view_harvest_log);
        //recyclerView.smoothScrollToPosition(0);

        mLogAdapter = new HarvestLogAdapter(this);

        //recyclerView.setLayoutManager(new LinearLayoutManager(this));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mLogAdapter);

        mSpinnerHarvestNo = (Spinner) findViewById(R.id.spinner_harvest_no);
        mSpinnerHarvestNo.setFocusable(true);
        //mSpinnerHarvestNo.setFocusableInTouchMode(true);

        mEditTextSuperJumbo = (EditText) findViewById(R.id.super_jumbo_et);
        mEditTextJumbo = (EditText) findViewById(R.id.jumbo_et);
        mEditTextLarge = (EditText) findViewById(R.id.large_et);


        // To get the ungraded harvest ID's in the spinner, latest harvest first
        Cursor cursor;
        SqlDbHelper dbHelper = new SqlDbHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] column = {LogContract.COLUMN_HARVEST_ID};
        String where = LogContract.COLUMN_HARVEST_GRADED + " is null or " + COLUMN_HARVEST_GRADED + " = ? ";
        String sortOrder = COLUMN_HARVEST_ID + " DESC";
        cursor = db.query(TABLE_LOGS, column, where, new String[]{""}, null, null, sortOrder);

        ArrayList<Integer> ungradedHarvests = new ArrayList<>();
        cursor.moveToFirst();
        Log.i(TAG, "cursor count " + cursor.getCount());
        while (!cursor.isAfterLast()) {
            Log.i(TAG, "cursor ints " + cursor.getInt(0));
            ungradedHarvests.add(cursor.getInt(0));
            cursor.moveToNext();
        }
        cursor.close();
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ungradedHarvests);


        mSpinnerHarvestNo.setAdapter(adapter);
        mSpinnerHarvestNo.setOnTouchListener(this);
        mEditTextSuperJumbo.setOnTouchListener(this);
        mEditTextJumbo.setOnTouchListener(this);
        mEditTextLarge.setOnTouchListener(this);


        // TODO spinner
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        Log.i(TAG, "getResFromApi");
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            Log.i(TAG, "name i getResultFrom: " + mCredential.getSelectedAccountName());
            mCredential.setSelectedAccountName(mGoogleAccount.getAccount().name);
            Log.i(TAG, "name i getResultFrom: " + mCredential.getSelectedAccountName());

            Log.i(TAG, "mService etter init i getResFromAp : " + (mService == null));
        } else if (!isDeviceOnline()) { // TODO toast i thread? kanskje ikke i thread nu
            Toast.makeText(this, "NO INTERNET CONNECTION mvh Satan", Toast.LENGTH_SHORT).show();
        }
    }

    private void postGradingData() {
        String spreadsheetId;
        String range;
        spreadsheetId = "1zx1lZfMgQ3z_Ip-6QEnSK0W9YkrRoBI0xmYktvxDrvA"; // TODO if sheet doesn't exist

        String name = mCredential.getSelectedAccountName();
        Object spinnerHarvestNo = mSpinnerHarvestNo.getSelectedItem();

        int selectedHarvestNo = (Integer) mSpinnerHarvestNo.getSelectedItem();

        if (mExistingRows == null || mExistingRows.getValues() == null) {
            //TODO bare finish her? for å unngå overwriting
            toastFromThread("Failed to read the online spreadsheet");
            finish();
            return;
        }
        List<Object> currentRow = mExistingRows.getValues().get(selectedHarvestNo);
        //TODO kun heltall???
        // Get the integer value of current row's registered weight, removing 'kg'
        int registeredCatch = Integer.valueOf((currentRow.get(4).toString()).replaceAll("\\D+", ""));


        String large = (mEditTextLarge.getText().toString().isEmpty()) ? "0" : mEditTextLarge.getText().toString();
        String jumbo = (mEditTextJumbo.getText().toString().isEmpty()) ? "0" : mEditTextJumbo.getText().toString();
        String superJumbo = (mEditTextSuperJumbo.getText().toString().isEmpty()) ? "0" : mEditTextSuperJumbo.getText().toString();

        int loss = registeredCatch - (Integer.valueOf(large) + Integer.valueOf(jumbo) + Integer.valueOf(superJumbo)); //TODO funke?

        String currentHarvestNo = String.valueOf(selectedHarvestNo + 1); // +1 cos row 1 == labels
        range = "sheet4!G" + currentHarvestNo + ":L" + currentHarvestNo; //TODO R.string

        List<List<Object>> values = new ArrayList<>();

        List<Object> gradingData = new ArrayList<>();

        // TODO update DB

        gradingData.add(getDate());
        //gradingData.add(getTime());
        // catchData.add(location);
        gradingData.add(large + " kg");
        gradingData.add(jumbo + " kg");
        gradingData.add(superJumbo + " kg");
        gradingData.add(mGoogleAccount.getDisplayName());
        gradingData.add(loss + " kg");

        values.add(gradingData);

        //Create the valuerange object and set its fields
        ValueRange valueRange = new ValueRange();
        valueRange.setMajorDimension("ROWS");
        valueRange.setRange(range);
        valueRange.setValues(values);

        String toastMessage;
        if (mService == null) {

            Log.i(TAG, "mService Null, fnish");
            toastMessage = "Failed to register catch!";
            toastFromThread(toastMessage);
            finish();
            return;
        }
        try {
            this.mService.spreadsheets().values()
                    .append(spreadsheetId, range, valueRange)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
        } catch (NullPointerException | IOException e) {
            Log.i(TAG, e.getMessage());
        }
//        getSupportLoaderManager().restartLoader(0,null,this);
        showSummaryDialog();
        Log.i(TAG, "new value 0: " + values.get(0) + "\n" + mExistingRows.getValues().get(selectedHarvestNo));
        mExistingRows.getValues().add(selectedHarvestNo, values.get(0));
        Log.i(TAG, "new value 0: " + values.get(0) + "\n" + mExistingRows.getValues().get(selectedHarvestNo));
        Utilities.updateDbSingle(this, valueRange, selectedHarvestNo);
       /* mExistingRows.getValues().add(selectedHarvestNo,values.get(0));
        Utilities.updateDb(this,mExistingRows);*/
        //Utilities.updateDb(this, mExistingRows.setValues(values));  // TODO only update the db with new values instead
        toastMessage = "Harvest number " + selectedHarvestNo + " graded";
        toastFromThread(toastMessage);
        finish();
    }

    public void postWeighingData() {
        String spreadsheetId;
        String range;
        spreadsheetId = "1zx1lZfMgQ3z_Ip-6QEnSK0W9YkrRoBI0xmYktvxDrvA";
        range = "sheet4!A1:F1";

        int harvestNum;
        Log.i(TAG, "mExistingRows.getValues == null " + (mExistingRows == null));
        if (mExistingRows == null || mExistingRows.getValues() == null) {
            toastFromThread("Failed to read the spreadsheet");
            finish();
            return;
        } else harvestNum = mExistingRows.getValues().size();
        // TODO can be null, Må updatere mExistingRows etter registrering

        List<List<Object>> values = new ArrayList<>();

        List<Object> catchData = new ArrayList<>();

        String location = null;
        try {
            location = Utilities.getLocationUrl();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        String snailCatch = (mUserInputCatch.getText().toString().isEmpty()) ? "0" : mUserInputCatch.getText().toString();
        String harvestNumber = String.valueOf(harvestNum);
        catchData.add(harvestNumber);
        catchData.add(getDate());
        catchData.add(getTime());
        catchData.add(location);
        catchData.add(snailCatch + " kg");
        catchData.add(mGoogleAccount.getDisplayName());

        values.add(catchData);

        //Create the valuerange object and set its fields
        ValueRange valueRange = new ValueRange();
        valueRange.setMajorDimension("ROWS");
        valueRange.setRange(range);
        valueRange.setValues(values);

        String toastMessage;
        Log.i(TAG, "mService Null? " + (mService == null));
        if (mService == null) {

            Log.i(TAG, "mService Null, fnish");
            toastMessage = "Failed to register catch!";
            toastFromThread(toastMessage);
            finish();
            return;
        }
        try {
            this.mService.spreadsheets().values()
                    .append(spreadsheetId, range, valueRange)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
        } catch (NullPointerException | IOException e) {
            Log.i(TAG, e.getMessage());
            toastMessage = "Failed to register catch!";
            toastFromThread(toastMessage);
            finish();
            return;

        }
        //  Utilities.updateDb(this, mExistingRows);
        showSummaryDialog(); // TODO?
        // updating the db immediately in order for the log to get updated (clumsy)
        mExistingRows.getValues().add(harvestNum, values.get(0));
        Utilities.updateDb(this, mExistingRows);
        // Utilities.updateDbSingle(this,valueRange, mExistingRows.getValues().size());
        // TODO lese spreadsheet igjen for å sjekke at faktisk oppdatert??
        toastMessage = "Harvest number " + harvestNum + " added";
        toastFromThread(toastMessage);
        finish();
    }

    private void toastFromThread(final String message) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // TODO occasional wrong format - due to threadsafe?
    private String getDate() {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        Date date = new Date(System.currentTimeMillis());

        return dateFormat.format(date);
    }

    // TODO sett i utils
    private String getTime() {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
        Date date = new Date(System.currentTimeMillis());

        return dateFormat.format(date);
    }

    private void showConfirmationDialog() {
        //TODO
    }

    private void showSummaryDialog() {
        //TODO
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param data        Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    //  mOutputText.setText( //TODO
                    //         "This app requires Google Play Services. Please install " +
                    //               "Google Play Services on your device and relaunch this app.");
                    Toast.makeText(this, "Install Google Play Services", Toast.LENGTH_SHORT).show();
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName = mGoogleAccount.getAccount().name;
                    // data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    Log.i(TAG, "onActResult" + "accName1: " + accountName + "\naccName2: " + mGoogleAccount.getAccount().name);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v instanceof EditText || v instanceof Spinner) {
            mCheckBox.setChecked(false);
        }
        if (v instanceof Spinner) {
            mSpinnerHarvestNo.requestFocus();
            mSpinnerHarvestNo.performClick();
            Log.i(TAG, "instance of Spinner");
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

            //  imm.hideSoftInputFromWindow(v.getWindowToken(), 0); // TODO forenkling ang edittexts?
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


}