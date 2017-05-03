package com.csulb.android.chatapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by vaibhavjain on 4/19/2017
 */


//Todo: Client fails if the server is not yet up. Change run() method to put socket.client() in loop.

public class WiFiClientThread extends Thread {
    private static final String TAG = "WiFiClientThread";
    //Todo: WiFiClientThread and WiFiServerThread can be merged to create one general thread there by saving system resources.
    private Socket socket;
    private String addr;
    private Handler handler;
    private InputStream inputStream;
    private OutputStream outputStream;

    public WiFiClientThread(@NonNull String addr, Handler handler) {
        this.addr = addr;
        this.handler = handler;
    }

    @Override
    public void run() {
        Log.d(TAG, "Starting Client thread");
        try {
            socket = new Socket();
            socket.bind(null);
            socket.connect(new InetSocketAddress(addr, 9999));
            Log.d(TAG, "Created a client connection");
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            byte[] bufferData = new byte[16384];
            int numOfPackets = 0;
            int datatype = 0;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (!this.isInterrupted()) {
                int numOfBytes = inputStream.read(bufferData);
                byte[] trimmedBufferData = Arrays.copyOf(bufferData, numOfBytes);
                bufferData = new byte[16384];
                ByteBuffer tempBuffer = ByteBuffer.wrap(trimmedBufferData);
                if (datatype == 0) {
                    datatype = tempBuffer.getInt();
                    Log.d(TAG, "Datatype: " + datatype);
                }
                if (numOfPackets == 0) {
                    numOfPackets = tempBuffer.getInt();
                    Log.d(TAG, "Packets size: " + numOfPackets);
                }
                byte[] dst = new byte[tempBuffer.remaining()];
                tempBuffer.get(dst);
                bos.write(dst);
                //Following condition checks if we have received all necessary bytes to construct a message out of it.
                if (bos.size() == numOfPackets) {
                    //For Text and Audio notes
                    if (datatype != 2) {
                        Log.d(TAG, "Data: " + new String(bos.toByteArray(), Charset.defaultCharset()));
                        Message msg = handler.obtainMessage(MessageConstants.MESSAGE_READ, -1, datatype, bos.toByteArray());
                        msg.sendToTarget();
                    } else {    //For images - Special check because we have to decode the image from Base64.
                        String decodedString = new String(bos.toByteArray(), Charset.defaultCharset());
                        Log.d(TAG, "Image Base64 decoded string: " + decodedString);
                        byte[] decodedStringArray = Base64.decode(decodedString, Base64.DEFAULT);
                        Bitmap bp = BitmapFactory.decodeByteArray(decodedStringArray, 0, decodedStringArray.length);
                        Message msg = handler.obtainMessage(MessageConstants.MESSAGE_READ, -1, datatype, bp);
                        msg.sendToTarget();
                    }
                    //Re-initialize for the next message.
                    datatype = 0;
                    numOfPackets = 0;
                    bos = new ByteArrayOutputStream();
                }
            }
        } catch(Exception e) {
            Log.d(TAG, "Unable to create a client connection", e);
        }
    }

    public void cancel(){
        this.interrupt();
        try {
            if (socket != null) {
                socket.close();
            }
        } catch(Exception e){
            Log.e(TAG, "Unable to close socket", e);
        }

    }

    public void write(byte[] bytes, String datatype) {
        try {
            Message writtenMsg = null;
            ByteArrayOutputStream tempOutputStream = new ByteArrayOutputStream();
            ByteBuffer tempBuffer = ByteBuffer.allocate(bytes.length + 8);
            if (datatype.equals(MessageConstants.DATATYPE_IMAGE)) {
                tempBuffer.putInt(2);
                ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
                imageStream.write(bytes);
                String decodedString = new String(imageStream.toByteArray(), Charset.defaultCharset());
                byte[] decodedStringArray = Base64.decode(decodedString, Base64.DEFAULT);
                Bitmap bp = BitmapFactory.decodeByteArray(decodedStringArray, 0, decodedStringArray.length);
                writtenMsg = handler.obtainMessage(MessageConstants.MESSAGE_WRITE, -1, 2, bp);
                imageStream.close();
            } else if (datatype.equals(MessageConstants.DATATYPE_TEXT)) {
                tempBuffer.putInt(1);
                writtenMsg = handler.obtainMessage(MessageConstants.MESSAGE_WRITE, -1, 1, bytes);
            } else if (datatype.equals(MessageConstants.DATATYPE_FILE)) {
                tempBuffer.putInt(3);
                writtenMsg = handler.obtainMessage(MessageConstants.MESSAGE_WRITE, -1, 3, bytes);
            }
            Log.d(TAG, "Sending size: " + bytes.length);
            tempBuffer.putInt(bytes.length);
            Log.d(TAG, "Sending data: " + new String(bytes, Charset.defaultCharset()));
            tempBuffer.put(bytes);
            //TODO: Redundant code. tempOutputStream should be removed!
            tempOutputStream.write(tempBuffer.array());
            outputStream.write(tempOutputStream.toByteArray());
            if (writtenMsg != null) {
                writtenMsg.sendToTarget();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);

            Message writeErrorMsg = handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString("toast", "Device disconnected. Couldn't send data to the other device");
            writeErrorMsg.setData(bundle);
            handler.sendMessage(writeErrorMsg);
        }
    }
}
