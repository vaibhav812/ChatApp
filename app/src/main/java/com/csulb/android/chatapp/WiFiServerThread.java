package com.csulb.android.chatapp;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by vaibhavjain on 4/19/2017
 */

public class WiFiServerThread extends Thread {
    private static final String TAG = "WiFiServerThread";
    private Context ctx;
    private Handler handler;
    public static int localPort;
    ServerSocket serverSocket;

    WiFiServerThread(){//Context ctx, Handler handler) {
        //this.ctx = ctx;
        //this.handler = handler;
    }

    @Override
    public void run() {
        //while() {
            try{
                serverSocket = new ServerSocket(9999);
                localPort = serverSocket.getLocalPort();
                Log.d(TAG, "Starting server thread");
                Socket client = serverSocket.accept();

                Log.d(TAG, "Got the request!");

            } catch(IOException e){
                Log.e(TAG, "IOException occured while creating/accepting requests in wifi server thread", e);
            }
        //}
    }

    public void cancel(){
        try {
            serverSocket.close();
        } catch(Exception e){
            Log.e(TAG, "Unable to close socket", e);
        }

    }
}
