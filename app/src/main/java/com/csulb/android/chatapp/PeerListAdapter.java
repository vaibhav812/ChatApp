package com.csulb.android.chatapp;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by vaibhavjain on 4/19/2017
 */

public class PeerListAdapter extends ArrayAdapter<WifiP2pDevice>{
    private Context ctx;

    public PeerListAdapter(Context ctx, int resource){
        super(ctx, resource);
        this.ctx = ctx;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        WifiP2pDevice device = (WifiP2pDevice) getItem(position);

        LayoutInflater inflater =(LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.peer_list_layout, null);

        TextView textView = (TextView) view.findViewById(R.id.wifi_peer_list_item);

        if(device !=null && !device.deviceName.equals("")) {
            textView.setText(device.deviceName);
        } else if(device != null) {
            textView.setText(device.deviceAddress);
        }
        return view;
    }
}
