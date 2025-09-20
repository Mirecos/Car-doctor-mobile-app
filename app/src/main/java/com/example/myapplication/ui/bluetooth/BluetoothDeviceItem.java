package com.example.myapplication.ui.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

public class BluetoothDeviceItem {
    
    private final BluetoothDevice device;
    private final String name;
    private final String address;
    private final int rssi;
    
    public BluetoothDeviceItem(BluetoothDevice device) {
        this.device = device;
        this.address = device.getAddress();
        
        // Handle device name with permission check
        String deviceName = "Unknown Device";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For Android 12+, we need BLUETOOTH_CONNECT permission
                deviceName = device.getName();
            } else {
                // For older versions, use the name directly
                deviceName = device.getName();
            }
            
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = "Unknown Device";
            }
        } catch (SecurityException e) {
            // Permission not granted, use default name
            deviceName = "Unknown Device";
        }
        
        this.name = deviceName;
        this.rssi = 0; // RSSI not available in basic discovery
    }
    
    public BluetoothDevice getDevice() {
        return device;
    }
    
    public String getName() {
        return name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public int getRssi() {
        return rssi;
    }
    
    public String getDeviceTypeString() {
        if (device == null) return "Unknown";
        
        int type = device.getType();
        switch (type) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                return "Classic";
            case BluetoothDevice.DEVICE_TYPE_LE:
                return "Low Energy";
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                return "Dual Mode";
            default:
                return "Unknown";
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BluetoothDeviceItem that = (BluetoothDeviceItem) obj;
        return address != null ? address.equals(that.address) : that.address == null;
    }
    
    @Override
    public int hashCode() {
        return address != null ? address.hashCode() : 0;
    }
}