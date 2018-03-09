package com.example.johnson.mibandtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.johnson.mibandtest.CustomBluetoothProfile;

import java.util.Arrays;
import java.util.Set;


// packages below are for google sheets and barchart
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.sheets.v4.SheetsScopes;

import com.google.api.services.sheets.v4.model.*;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

// packages below are for histogram
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;



public class MainActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;  // setup the bluetooth
    BluetoothDevice MiBand2Device; // define MI Band 2 device
    BluetoothGatt mbluetoothGatt; // define MI Band bluetooth gatt to connect
    Button StartConectButton,BatteryButton, mCallApiButton,mBarChartButton;

    TextView MiBand2Name, MiBand2HardwareAddress, MiBand2BatteryData,mOutputWeeklySteps;
    String resultRealTimeBattery;

    // Initialinize of barchart and google sheet API
    GoogleAccountCredential mCredential;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS_READONLY };// if want to write google sheet, need to change it

    BarChart barChart;   // create a barchart
    List<String> DateResults ;
    List<Float> WeekStepsResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialObjects();
        initilaizeComponents();
        initializeEvents();

    }



    private void initialObjects() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();   //get bluetooth of device
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }
        else if  (!mBluetoothAdapter.isEnabled()) { // if it supports bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            startActivity(enableBtIntent);  // will have option to user whether to open the bluetooth or not
        }

        MiBand2Name = (TextView) findViewById(R.id.MiNameTextView);
       // MiBand2HardwareAddress = (TextView) findViewById(R.id.MiAddTextView);
        MiBand2BatteryData = (TextView) findViewById(R.id.MiBatteryTextView);
        StartConectButton = (Button) findViewById(R.id.StartConnectbtn);
        BatteryButton = (Button) findViewById(R.id.Batterybtn);

        // buttons and TextView for barchart with googlesheetapi
        mCallApiButton = (Button) findViewById(R.id.Btncall);
        mOutputWeeklySteps = (TextView) findViewById(R.id.TextViewSteps);
        mBarChartButton = (Button) findViewById(R.id.BtnBarChart);

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());


    }

    private void initilaizeComponents(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices(); // get set of history paired devices

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().contains("MI Band 2")){
                    //MiBand2Name.setText(device.getName());
                   // MiBand2HardwareAddress.setText(device.getAddress()); // MAC address
                    MiBand2Device = mBluetoothAdapter.getRemoteDevice(device.getAddress());


                }

            }
        }

    }
    void getBatteryStatus() {
        MiBand2BatteryData.setText("...");
        BluetoothGattCharacteristic bchar = mbluetoothGatt.getService(CustomBluetoothProfile.Basic.service)
                .getCharacteristic(CustomBluetoothProfile.Basic.batteryCharacteristic);
        //if()
        if (!mbluetoothGatt.readCharacteristic(bchar)) { // if delete this line, then can not read the byte data from the battery
            Toast.makeText(this, "Failed get battery info", Toast.LENGTH_SHORT).show();
        }

    }
    private void initializeEvents() {
        StartConectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConnecting();
            }
        });
        BatteryButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                getBatteryStatus();
                //MakeRequestTask.updateDataFromApi();
            }
        });

        // buttons initialize of Google sheet api and barchart
        mCallApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallApiButton.setEnabled(false);
                mOutputWeeklySteps.setText("");
                getResultsFromApi();
                mCallApiButton.setEnabled(true);
            }
        });
        mBarChartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBarCharting();
            }
        });
    }

    private void startConnecting() {
        mbluetoothGatt = MiBand2Device.connectGatt(this,true, bluetoothGattCallback);
        //getBatteryStatus();
    }

    void stateConnected() {
        mbluetoothGatt.discoverServices();
        MiBand2Name.setText("Connected");
    }

    void stateDisconnected() {
        mbluetoothGatt.disconnect();
        MiBand2Name.setText("Disconnected");
    }

    final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.v("test", "onConnectionStateChange");

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                stateConnected();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                stateDisconnected();
                startConnecting();
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.v("test", "onServicesDiscovered");
           // listenHeartRate();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.v("test", "onCharacteristicRead");
            byte[] data = characteristic.getValue();
            //MiBand2Data.setText(Arrays.toString(data));
           // MiBand2Data.setText(Arrays.toString(()));
            resultRealTimeBattery = String.valueOf(data[1]);
            MiBand2BatteryData.setText(String.valueOf(data[1]));
           // Arrays.t
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.v("test", "onCharacteristicWrite");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.v("test", "onCharacteristicChanged");
            byte[] data = characteristic.getValue();
            MiBand2BatteryData.setText(Arrays.toString(data));
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.v("test", "onDescriptorRead");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.v("test", "onDescriptorWrite");
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.v("test", "onReliableWriteCompleted");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.v("test", "onReadRemoteRssi");
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.v("test", "onMtuChanged");
        }
    };

    // functions for google sheet API and barchart

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            mOutputWeeklySteps.setText("No network connection available.");
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputWeeklySteps.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
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
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
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
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    //@Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    // @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
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
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
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
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
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

    /**
     * An asynchronous task that handles the Google Sheets API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.sheets.v4.Sheets mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Sheets API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Sheets API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                updateDataFromApi(); // update the google sheet
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch information in a spreadsheet:
         * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
         * @return List of names and majors
         * @throws IOException
         */
        public List<String> getDataFromApi() throws IOException {
            //  String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms"; // original google sheet ID
            String spreadsheetId = "1zRYonLSPg7KqeckrHQfFfufqeVp73c6_IDGGRIPvhhA";   // weekly steps sheet ID
            String range = "Sheet2!A2:B";
            List<String> results = new ArrayList<String>();
            DateResults = new ArrayList<String>(); // get the date into the global DateResults
            WeekStepsResults = new ArrayList<Float>();
            ValueRange response = this.mService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            if (values != null) {
                results.add("Date, Steps");
                for (List row : values) {

                    results.add(row.get(0) + ", " + row.get(1));
                    DateResults.add(row.get(0) + "");
                    WeekStepsResults.add(Float.parseFloat(row.get(1).toString())); // change the steps into string, then float
                    //
                }
            }
            return results;
        }

        /**
         * Update battery and real-time steps in a spreadsheet:
         * https://docs.google.com/spreadsheets/d/1zRYonLSPg7KqeckrHQfFfufqeVp73c6_IDGGRIPvhhA/edit
         *
         * @throws IOException
         */
        public void updateDataFromApi() throws IOException {
            //  String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms"; // original google sheet ID
            String spreadsheetId = "1zRYonLSPg7KqeckrHQfFfufqeVp73c6_IDGGRIPvhhA";   // weekly steps sheet ID
            String range = "Sheet1!C2:D";
            Object a1 = new Object();
            a1 = resultRealTimeBattery;
            Object b1 = new Object();
           // b1 = "TEST Row 1 Column B";

          /*  Object a2 = new Object();
            a2 = "TEST Row 2 Column A";
            Object b2 = new Object();
            b2 = "TEST Row 2 Column B";*/

            ValueRange valueRange = new ValueRange();
            valueRange.setValues(
                    Arrays.asList(
                            Arrays.asList(a1, null),
                            Arrays.asList(null, null)));
                   // Arrays.asList(a1, b1));
            this.mService.spreadsheets().values().update(spreadsheetId, range, valueRange)
                    .setValueInputOption("RAW")
                    .execute();
        }





        @Override
        protected void onPreExecute() {
            mOutputWeeklySteps.setText("");
            //   mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            //  mProgress.hide();
            if (output == null || output.size() == 0) {
                mOutputWeeklySteps.setText("No results returned.");
            } else {
                output.add(0, "Data retrieved using the Google Sheets API:");
                mOutputWeeklySteps.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            //   mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputWeeklySteps.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputWeeklySteps.setText("Request cancelled.");
            }
        }
    }

    private void startBarCharting(){

        barChart = (BarChart) findViewById(R.id.bargraph);

        ArrayList<BarEntry> barEntries = new ArrayList<>();
        Float i = 0f;
        for (Float WeekStepsData: WeekStepsResults){
            barEntries.add(new BarEntry(i,WeekStepsData));
            i = i+1;
        }
     /*   barEntries.add(new BarEntry(0f, 675f));
        barEntries.add(new BarEntry(1f, 49f));
        barEntries.add(new BarEntry(2f, 0f));
        barEntries.add(new BarEntry(3f, 2798f));
        barEntries.add(new BarEntry(4f, 1093f));
        barEntries.add(new BarEntry(5f, 4889f));
        barEntries.add(new BarEntry(6f, 3394f));
        barEntries.add(new BarEntry(7f, 391f));
*/

        BarDataSet barDataSet = new BarDataSet(barEntries, "Steps");
        barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);  // set the graph into colorful



        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add((IBarDataSet)barDataSet);




        BarData data = new BarData(dataSets);

        IAxisValueFormatter formatter = new IAxisValueFormatter() {

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                ArrayList<String> xData = new ArrayList<>();
                for (String DateData: DateResults){
                    xData.add(DateData);
                }
           /*     xData.add("02/22");
                xData.add("02/23");
                xData.add("02/24");
                xData.add("02/25");
                xData.add("02/26");
                xData.add("02/27");
                xData.add("02/28");
                xData.add("03/01");*/
                return xData.get((int)value);
            }

            // we don't draw numbers, so no decimal digits needed
            public int getDecimalDigits() {  return 0; }
        };

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // put the time into bottom
        xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
        xAxis.setValueFormatter(formatter);


        // data.setBarWidth(0.9f); // set custom bar width
        barChart.setData(data);
        barChart.setFitBars(true); // make the x-axis fit exactly all bars
        barChart.invalidate(); // refresh

        //  BarData theData = new BarData(barDataSet);
        //  barChart.setData(theData);

        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(true);
        barChart.setFitBars(true);
    };
}
