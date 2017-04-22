package com.csulb.android.chatapp;

import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by vaibhavjain on 4/17/2017
 */

public class ConnectedThread extends Thread {
    private final BluetoothSocket bluetoothSocket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private Handler handler;
    private final String TAG = "ConnectedThread";

    public ConnectedThread(BluetoothSocket socket, Handler handler) {
        bluetoothSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }
        inputStream =  tmpIn;
        outputStream = tmpOut;
        this.handler = handler;
    }

    public void write(byte[] bytes, String datatype) {
        try {
            Message writtenMsg = null;
            if(datatype.equals(MessageConstants.DATATYPE_IMAGE)) {
                outputStream.write(MessageConstants.START_IMAGE_INDICATOR.getBytes());
                outputStream.flush();
                outputStream.write(String.valueOf(Math.ceil(((float)bytes.length)/990)).getBytes());
                outputStream.flush();
                outputStream.write(bytes);
                outputStream.flush();
                ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
                imageStream.write(bytes);
                String decodedString = new String(imageStream.toByteArray(), Charset.defaultCharset());
                byte[] decodedStringArray = Base64.decode(decodedString, Base64.DEFAULT);
                Bitmap bp = BitmapFactory.decodeByteArray(decodedStringArray, 0, decodedStringArray.length);
                writtenMsg = handler.obtainMessage(MessageConstants.MESSAGE_WRITE, -1, 2, bp);
            } else if(datatype.equals(MessageConstants.DATATYPE_TEXT)) {
                outputStream.write(MessageConstants.TEXT_SEND_INDICATOR.getBytes());
                outputStream.flush();
                outputStream.write("1.0".getBytes());
                outputStream.flush();
                outputStream.write(bytes);
                outputStream.flush();
                writtenMsg = handler.obtainMessage(MessageConstants.MESSAGE_WRITE, -1, 1, bytes);
            } else if(datatype.equals(MessageConstants.DATATYPE_FILE)) {
                outputStream.write(MessageConstants.FILE_SEND_INDICATOR.getBytes());
                outputStream.flush();
                outputStream.write(String.valueOf(Math.ceil(((float)bytes.length)/990)).getBytes());
                outputStream.flush();
                outputStream.write(bytes);
                outputStream.flush();
                writtenMsg = handler.obtainMessage(MessageConstants.MESSAGE_WRITE, -1, 3, bytes);
            }
            if(writtenMsg != null) {
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

    public void run() {
        int numOfBytes;
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        Bitmap bp = null;
        int counter = 0;
        int numOfPackets = -1;
        boolean isDatatypeReceived = false, isPacketReceived = false;
        int type = -1;
        while (true) {
            try {
                byte[] bufferData = new byte[1024];
                numOfBytes = inputStream.read(bufferData);
                byte[] trimmedBufferData = Arrays.copyOf(bufferData, numOfBytes);
                bufferData = new byte[1024];
                if(!isDatatypeReceived && !isPacketReceived) {
                    if(Arrays.equals(MessageConstants.TEXT_SEND_INDICATOR.getBytes(), trimmedBufferData)) {
                        type = 1;
                    } else if(Arrays.equals(MessageConstants.START_IMAGE_INDICATOR.getBytes(), trimmedBufferData)) {
                        type = 2;
                    } else if(Arrays.equals(MessageConstants.FILE_SEND_INDICATOR.getBytes(), trimmedBufferData)) {
                        type = 3;
                    }
                    isDatatypeReceived = true;
                } else if(isDatatypeReceived && !isPacketReceived) {
                    isPacketReceived = true;
                    String str = new String(trimmedBufferData);
                    String[] strParts = str.split(".0");
                    Double d = Double.parseDouble(strParts[0]);
                    numOfPackets = d.intValue();
                    if(strParts.length > 1 && strParts[1].length() > 0) {
                        dataStream.write(strParts[1].getBytes());
                        counter++;
                        if(numOfPackets == 1){
                            Message readMsg = handler.obtainMessage(MessageConstants.MESSAGE_READ, numOfBytes, type, strParts[1].getBytes());
                            readMsg.sendToTarget();
                            isDatatypeReceived = false;
                            isPacketReceived = false;
                            dataStream.reset();
                            counter = 0;
                            numOfPackets = -1;
                        }
                    }
                } else if(isDatatypeReceived && isPacketReceived && counter == numOfPackets - 1) {
                    if(type == 1 ){
                        Message readMsg = handler.obtainMessage(MessageConstants.MESSAGE_READ, numOfBytes, type, trimmedBufferData);
                        readMsg.sendToTarget();
                    } else if(type == 2) {
                        dataStream.write(trimmedBufferData);
                        dataStream.flush();
                        String decodedString = new String(dataStream.toByteArray(), Charset.defaultCharset());
                        byte[] decodedStringArray = Base64.decode(decodedString, Base64.DEFAULT);
                        bp = BitmapFactory.decodeByteArray(decodedStringArray, 0, decodedStringArray.length);
                        Message readMsg = handler.obtainMessage(MessageConstants.MESSAGE_READ, numOfBytes, type, bp);
                        readMsg.sendToTarget();
                    } else if(type == 3) {
                        dataStream.write(trimmedBufferData);
                        dataStream.flush();
                        Message readMsg = handler.obtainMessage(MessageConstants.MESSAGE_READ, numOfBytes, type, dataStream.toByteArray());
                        readMsg.sendToTarget();
                    }
                    dataStream.reset();
                    counter = 0;
                    numOfPackets = -1;
                    isDatatypeReceived = false;
                    isPacketReceived = false;
                }
                else if(isDatatypeReceived && isPacketReceived && counter < numOfPackets - 1) {
                    dataStream.write(trimmedBufferData);
                    dataStream.flush();
                    counter++;
                }
            } catch (IOException e) {
                Log.d(TAG, "Input stream was disconnected", e);
                break;
            }
        }
    }

    public void cancel() {
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }
}
