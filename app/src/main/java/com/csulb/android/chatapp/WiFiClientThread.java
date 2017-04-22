package com.csulb.android.chatapp;

import android.util.Log;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by vaibhavjain on 4/19/2017
 */

public class WiFiClientThread extends Thread {
    Socket socket;
    InetAddress addr;
    private static final String TAG = "WiFiClientThread";
    public WiFiClientThread(InetAddress addr) {
        this.addr = addr;
    }

    @Override
    public void run() {
        try {
            socket = new Socket();
            socket.bind(null);
            socket.connect(new InetSocketAddress(addr.getHostAddress(), 9999), 500);
            Log.d(TAG, "Created a client connection");
        } catch(Exception e) {
            Log.d(TAG, "Unable to create a client connection", e);
        }
    }

    public void cancel(){
        try {
            socket.close();
        } catch(Exception e){
            Log.e(TAG, "Unable to close socket", e);
        }

    }
}
