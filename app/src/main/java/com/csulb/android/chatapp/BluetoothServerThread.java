package com.csulb.android.chatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;

/**
 * Created by vaibhavjain on 4/16/2017
 */

class BluetoothServerThread extends Thread {
    private final BluetoothServerSocket serverSocket;
    private final String TAG = "BluetoothServerThread";
    Handler handler;
    Context ctx = null;
    public static BluetoothSocket socket;

    BluetoothServerThread(Context ctx, BluetoothAdapter adapter, Handler handler) {
        BluetoothServerSocket tmp = null;
        try {
            tmp = adapter.listenUsingRfcommWithServiceRecord(BluetoothConnectActivity.APP_NAME, BluetoothConnectActivity.APP_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket's listen() method failed", e);
        }
        serverSocket = tmp;
        this.ctx = ctx;
        this.handler = handler;
    }

    public void run() {
        //BluetoothSocket socket = null;
        while (true) {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                Log.e(TAG, "Socket's accept() method failed", e);
                break;
            }

            if (socket != null) {
                try {
                    Log.e(TAG, "SERVER CONNECTED");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(ctx, ChatActivity.class);
                            intent.putExtra(MessageConstants.CONNECTION_MODE, MessageConstants.CONNECTION_MODE_BLUETOOTH);
                            intent.putExtra(MessageConstants.IS_SERVER, true);
                            ctx.startActivity(intent);
                        }
                    });
                } catch(Exception ioe) {
                    Log.e(TAG, "Error occurred while closing socket ", ioe);
                }
                break;
            }
        }
    }

    public void cancel() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }
}
