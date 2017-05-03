package com.csulb.android.chatapp;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

//TODO: ChatActivity and WiFiChatActivity should be merged and only 1 general ChatActivity should be created.
public class WiFiDirectActivity extends AppCompatActivity {
    private static String TAG = "WiFiDirectActivity";
    WifiP2pManager wifiManager = null;
    WifiP2pManager.Channel channel = null;
    ConnectionBroadcastReceiver brdcastreceiver = null;
    IntentFilter intentFilter = new IntentFilter();
    WifiManager manager;
    WiFiServerThread serverThread;
    WiFiClientThread clientThread;
    ListView peerListView;
    PeerListAdapter peerListAdapter;
    boolean unregisterBReceiver = false;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageConstants.PEER_LIST:
                    ArrayList<WifiP2pDevice> deviceList = (ArrayList<WifiP2pDevice>) msg.obj;
                    for (WifiP2pDevice device : deviceList) {
                        int position = peerListAdapter.getPosition(device);
                        if (position < 0) {
                            peerListAdapter.add(device);
                        }
                    }
                    peerListAdapter.notifyDataSetChanged();
                    break;
                case MessageConstants.UNREGISTER_RECEIVER:
                    unregisterBReceiver = (Boolean) msg.obj;
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_direct);

        unregisterBReceiver = false;
        peerListView = (ListView) findViewById(R.id.peer_list_view);
        peerListAdapter = new PeerListAdapter(WiFiDirectActivity.this, R.id.wifi_peer_list_item);
        peerListView.setAdapter(peerListAdapter);
        peerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WifiP2pDevice device = (WifiP2pDevice) parent.getItemAtPosition(position);
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                wifiManager.connect(channel, config, new WifiP2pManager.ActionListener(){

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Connection successful", Toast.LENGTH_SHORT).show();
                        //finish();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(WiFiDirectActivity.this, "Connection failure reason: " + reason, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        wifiManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        channel = wifiManager.initialize(this, getMainLooper(), null);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        brdcastreceiver = new ConnectionBroadcastReceiver(WiFiDirectActivity.this, wifiManager, channel, handler, unregisterBReceiver);
        registerReceiver(brdcastreceiver, intentFilter);
        Log.d(TAG, "Registered Wifi broadcast receiver");
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(brdcastreceiver);
        Log.d(TAG, "Unregistered wifi broadcast receiver");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serverThread != null) {
            serverThread.cancel();
        }

        if (clientThread != null) {
            clientThread.cancel();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.wifi_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.btn_direct_enable:

                if (unregisterBReceiver) {
                    unregisterBReceiver = false;
                    brdcastreceiver = new ConnectionBroadcastReceiver(WiFiDirectActivity.this, wifiManager, channel, handler, unregisterBReceiver);
                    registerReceiver(brdcastreceiver, intentFilter);
                    Log.d(TAG, "Registered Wifi broadcast receiver");
                }

                if (wifiManager != null && channel != null) {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));

                } else {
                    Toast.makeText(WiFiDirectActivity.this, "Unable to get Wifi manager", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "channel or manager is null");
                }
                return true;
            case R.id.btn_direct_discover:

                if (unregisterBReceiver) {
                    unregisterBReceiver = false;
                    brdcastreceiver = new ConnectionBroadcastReceiver(WiFiDirectActivity.this, wifiManager, channel, handler, unregisterBReceiver);
                    registerReceiver(brdcastreceiver, intentFilter);
                    Log.d(TAG, "Registered Wifi broadcast receiver");
                }
                if(!manager.isWifiEnabled()) {
                    Toast.makeText(WiFiDirectActivity.this, "Wifi is off. Please turn it on",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                Log.d(TAG, "Starting Peer discovery");
                wifiManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
