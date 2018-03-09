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


public class MainActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;  // setup the bluetooth
    BluetoothDevice MiBand2Device; // define MI Band 2 device
    BluetoothGatt mbluetoothGatt; // define MI Band bluetooth gatt to connect
    Button StartConectButton,BatteryButton, CallSheetAPIButton, BarChartButton;

    TextView MiBand2Name, MiBand2HardwareAddress, MiBand2BatteryData;

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
}
