package com.example.myapplication.ui.bluetooth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

public class BluetoothDeviceAdapter extends ListAdapter<BluetoothDeviceItem, BluetoothDeviceAdapter.DeviceViewHolder> {
    
    public BluetoothDeviceAdapter() {
        super(DIFF_CALLBACK);
    }
    
    private static final DiffUtil.ItemCallback<BluetoothDeviceItem> DIFF_CALLBACK = 
        new DiffUtil.ItemCallback<BluetoothDeviceItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull BluetoothDeviceItem oldItem, @NonNull BluetoothDeviceItem newItem) {
                return oldItem.getAddress().equals(newItem.getAddress());
            }
            
            @Override
            public boolean areContentsTheSame(@NonNull BluetoothDeviceItem oldItem, @NonNull BluetoothDeviceItem newItem) {
                return oldItem.equals(newItem);
            }
        };
    
    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bluetooth_device, parent, false);
        return new DeviceViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothDeviceItem device = getItem(position);
        holder.bind(device);
    }
    
    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final TextView textDeviceName;
        private final TextView textDeviceAddress;
        private final TextView textDeviceType;
        
        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            textDeviceName = itemView.findViewById(R.id.text_device_name);
            textDeviceAddress = itemView.findViewById(R.id.text_device_address);
            textDeviceType = itemView.findViewById(R.id.text_device_type);
        }
        
        public void bind(BluetoothDeviceItem device) {
            textDeviceName.setText(device.getName());
            textDeviceAddress.setText(device.getAddress());
            textDeviceType.setText(device.getDeviceTypeString());
        }
    }
}