package com.example.thermographdemo;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by QLK on 20-4-22.
 */
public class DeviceAdapter extends ArrayAdapter<BluetoothDevice> {

    public DeviceAdapter(@NonNull Context context, List<BluetoothDevice> list) {
        super(context, android.R.layout.simple_list_item_1, android.R.id.text1, list);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final View view;
        final TextView text;

        if (convertView == null) {
            view = LayoutInflater.from(getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
        } else {
            view = convertView;
        }

        text = view.findViewById(android.R.id.text1);
        BluetoothDevice item = getItem(position);
        text.setText(item.getName() + "::" + item.getAddress());
        return view;
    }

}
