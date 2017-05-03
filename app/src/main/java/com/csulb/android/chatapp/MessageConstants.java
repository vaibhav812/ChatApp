package com.csulb.android.chatapp;

/**
 * Created by vaibhavjain on 4/17/2017
 */

public  class MessageConstants {

    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_TOAST = 3;
    public static final int PEER_LIST = 9;
    public static final int UNREGISTER_RECEIVER = 18;
    public static final String CONNECTION_MODE = "MODE";
    public static final String CONNECTION_MODE_BLUETOOTH = "Bluetooth";
    public static final String CONNECTION_MODE_WIFI = "WiFi-Direct";
    public static final String IS_SERVER = "isThisServer";
    public static final String DATATYPE_IMAGE = "image";
    public static final String DATATYPE_TEXT = "text";
    public static final String DATATYPE_FILE = "file";
    public static final String START_IMAGE_INDICATOR = "ChatAppImageSendingFIRSTArrayByt";
    public static final String END_IMAGE_INDICATOR = "ChatAppImageSendingLASTArrayByte";
    public static final String FILE_SEND_INDICATOR = "ChatApp.FileSending";
    public static final String TEXT_SEND_INDICATOR = "ChatApp.Text";
    public static final String EXTRA_OWNER_ADDR = "GroupOwner.Address";
    public static final String EXTRA_IS_GROUP_OWNER = "isGroupOwner";
}
