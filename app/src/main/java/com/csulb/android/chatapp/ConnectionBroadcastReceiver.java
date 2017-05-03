package com.csulb.android.chatapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Created by vaibhavjain on 3/28/2017
 */

public class ConnectionBroadcastReceiver extends BroadcastReceiver implements WifiP2pManager.PeerListListener{
    private WifiP2pManager mManager = null;
    private WifiP2pManager.Channel mChannel = null;
    private Context ctx = null;
    private String TAG = "WiFiBroadcastReceiver";
    private Handler handler;
    private boolean unregisterBReceiver;

    public ConnectionBroadcastReceiver(Context ctx, WifiP2pManager manager, WifiP2pManager.Channel channel, Handler handler,
                                       boolean unregiterBReceiver) {
        this.mManager = manager;
        mChannel = channel;
        this.ctx = ctx;
        this.handler = handler;
        this.unregisterBReceiver = unregiterBReceiver;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();
        switch(action){
            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                if(mManager != null){
                    Log.d(TAG, "Peer change broadcast received");
                    Log.d(TAG, "Requesting peers");
                    mManager.requestPeers(mChannel, this);
                }
                break;
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                Log.d(TAG, "Connection changed broadcast received");
                if(mManager == null) {
                    Log.e(TAG, "WiFiP2pManager is null");
                    return;
                }
                final NetworkInfo netInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if(netInfo.isConnected()) {
                    Log.d(TAG, "WiFi Direct Connected");
                    mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                        @Override
                        public void onConnectionInfoAvailable(WifiP2pInfo info) {
                            InetAddress addr = info.groupOwnerAddress;
                            Log.d(TAG, "Address is: " + addr.getHostAddress() + " " + info.isGroupOwner);

                            Message msg = handler.obtainMessage(MessageConstants.UNREGISTER_RECEIVER, true);
                            msg.sendToTarget();
                            if (!unregisterBReceiver) {
                                Intent chatIntent = new Intent();
                                chatIntent.setClass(context, WiFiChatActivity.class);
                                chatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                chatIntent.putExtra(MessageConstants.EXTRA_OWNER_ADDR, addr.getHostAddress());
                                chatIntent.putExtra(MessageConstants.EXTRA_IS_GROUP_OWNER, info.isGroupOwner);
                                context.startActivity(chatIntent);
                            }
                        }
                    });
                } else {
                    Log.d(TAG, "WiFi Direct not connected");
                }
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        ArrayList<WifiP2pDevice> deviceList = new ArrayList<WifiP2pDevice>();
        Log.d(TAG, "Peer list size: " + peers.getDeviceList().size());
        deviceList.addAll(peers.getDeviceList());
        Message msg = handler.obtainMessage(MessageConstants.PEER_LIST, deviceList);
        msg.sendToTarget();
    }
}
