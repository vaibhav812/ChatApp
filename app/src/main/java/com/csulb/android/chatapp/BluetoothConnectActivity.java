package com.csulb.android.chatapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnectActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    BluetoothManager bluetoothManager = null;
    BluetoothAdapter bluetoothAdapter = null;
    IntentFilter filter = null;
    DeviceListAdapter mAdapter = null;
    ListView deviceListView = null;
    BluetoothServerThread bluetoothServerThread = null;
    Handler handler;
    public static BluetoothSocket socket;
    public static final int REQUEST_ENABLE_BLUETOOTH = 10;
    public static final int REQUEST_COARSE_LOCATION = 11;
    public static final UUID APP_UUID = UUID.fromString("d0ffb520-2334-11e7-9598-0800200c9a66");
    public static final String APP_NAME = "ChatApp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        setContentView(R.layout.bluetooth_connect);

        deviceListView = (ListView) findViewById(R.id.device_list_view);
        mAdapter = new DeviceListAdapter(BluetoothConnectActivity.this, R.layout.device_list_row_layout);
        deviceListView.setAdapter(mAdapter);
        deviceListView.setOnItemClickListener(this);

        bluetoothManager =(BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        //bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothManager == null){
            Toast.makeText(this, "Bluetooth service unavailable!", Toast.LENGTH_SHORT).show();
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported by this device!", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!bluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
        }
        Log.e("BLUETOOTH", bluetoothAdapter.isEnabled() +"");

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                Log.e("HANDLER", msg.toString());
            }
        };

        bluetoothServerThread = new BluetoothServerThread(BluetoothConnectActivity.this, bluetoothAdapter, handler);
        bluetoothServerThread.start();
    }

    private final BroadcastReceiver discoverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("BROADCAST RECEIVED", intent.getAction());
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int position = mAdapter.getPosition(device);
                Log.e("Position" , String.valueOf(position));
                if(position < 0) {
                    mAdapter.add(device);
                    mAdapter.notifyDataSetChanged();
                }
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.e("DISCOVERY", "FINISHED");
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("BROADCAST", "REGISTERED");
        /*if(!bluetoothServerThread.isAlive()) {
            bluetoothServerThread.start();
        }*/
        registerReceiver(discoverReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("Server", "Shutting down");
        Log.e("BluetoothConnect", "BROADCAST STOPPED");
        unregisterReceiver(discoverReceiver);
        bluetoothServerThread.cancel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.bluetooth_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.discover_device:
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);
                break;
            case R.id.scan_devices:
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_COARSE_LOCATION);
                } else {
                    bluetoothAdapter.startDiscovery();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case REQUEST_COARSE_LOCATION:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                    bluetoothAdapter.startDiscovery();
                }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);
        socket= null;
        try {
            socket = device.createRfcommSocketToServiceRecord(BluetoothConnectActivity.APP_UUID);
            if(socket != null) {
                socket.connect();
            }
        } catch (IOException ioe) {
            Log.e("BluetoothConnect", "Socket's create() method failed", ioe);
        }
        if(socket != null) {
            Log.e("SOCKET CONNECTED", String.valueOf(socket.isConnected()));
            //ConnectedThread thread = new ConnectedThread(socket, handler);
            //thread.start();
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(MessageConstants.CONNECTION_MODE, MessageConstants.CONNECTION_MODE_BLUETOOTH);
            intent.putExtra(MessageConstants.IS_SERVER, false);
            startActivity(intent);
        }
    }
}
