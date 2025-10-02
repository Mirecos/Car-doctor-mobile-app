package com.example.myapplication.ui.bluetooth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

public class BluetoothDeviceAdapter extends ListAdapter<BluetoothDeviceItem, BluetoothDeviceAdapter.DeviceViewHolder> {
    
    public interface OnDeviceActionListener {
        void onPairUnpairClick(BluetoothDeviceItem device);
        void onConnectClick(BluetoothDeviceItem device);
    }
    
    private OnDeviceActionListener actionListener;
    
    public BluetoothDeviceAdapter() {
        super(DIFF_CALLBACK);
    }
    
    public void setOnDeviceActionListener(OnDeviceActionListener listener) {
        this.actionListener = listener;
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
        holder.bind(device, actionListener);
    }
    
    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final TextView textDeviceName;
        private final TextView textDeviceAddress;
        private final TextView textDeviceType;
        private final Button buttonPairUnpair;
        private final Button buttonConnect;
        
        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            textDeviceName = itemView.findViewById(R.id.text_device_name);
            textDeviceAddress = itemView.findViewById(R.id.text_device_address);
            textDeviceType = itemView.findViewById(R.id.text_device_type);
            buttonPairUnpair = itemView.findViewById(R.id.button_pair_unpair);
            buttonConnect = itemView.findViewById(R.id.button_connect);
        }
        
        public void bind(BluetoothDeviceItem device, OnDeviceActionListener listener) {
            textDeviceName.setText(device.getName());
            textDeviceAddress.setText(device.getAddress());
            textDeviceType.setText(device.getDeviceTypeString());
            
            // Update button states based on pairing status
            if (device.isPaired()) {
                buttonPairUnpair.setText(R.string.unpair);
                buttonConnect.setEnabled(true);
                if (device.isConnected()) {
                    buttonConnect.setText(R.string.disconnect);
                } else {
                    buttonConnect.setText(R.string.connect);
                }
            } else {
                buttonPairUnpair.setText(R.string.pair);
                buttonConnect.setEnabled(false);
                buttonConnect.setText(R.string.connect);
            }
            
            // Set click listeners
            buttonPairUnpair.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPairUnpairClick(device);
                }
            });
            
            buttonConnect.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConnectClick(device);
                }
            });
        }
    }
}