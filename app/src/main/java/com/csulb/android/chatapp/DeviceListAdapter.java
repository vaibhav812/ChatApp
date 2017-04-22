package com.csulb.android.chatapp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by vaibhavjain on 4/16/2017
 */

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

    DeviceListAdapter(Context ctx, int resource){
        super(ctx, resource);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if(view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.device_list_row_layout, null);
        }
        BluetoothDevice device = (BluetoothDevice) getItem(position);
        TextView textView = (TextView) view.findViewById(R.id.device_list_item);
        if(device != null && device.getName() != null && !device.getName().equals("")) {
            textView.setText(device.getName());
        } else if(device != null){
            textView.setText(device.getAddress());
        }
        return view;
    }
}
