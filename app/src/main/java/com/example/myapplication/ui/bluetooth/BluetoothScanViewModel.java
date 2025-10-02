package com.example.myapplication.ui.bluetooth;

import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BluetoothScanViewModel extends ViewModel {
    
    private final MutableLiveData<List<BluetoothDeviceItem>> devices = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> scanning = new MutableLiveData<>(false);
    private final Set<String> deviceAddresses = new HashSet<>();
    
    public LiveData<List<BluetoothDeviceItem>> getDevices() {
        return devices;
    }
    
    public LiveData<Boolean> isScanning() {
        return scanning;
    }
    
    public void setScanningState(boolean isScanning) {
        scanning.setValue(isScanning);
    }
    
    public void addDevice(BluetoothDevice device) {
        if (device == null || device.getAddress() == null) {
            return;
        }
        
        // Avoid duplicates
        if (deviceAddresses.contains(device.getAddress())) {
            return;
        }
        
        deviceAddresses.add(device.getAddress());
        
        List<BluetoothDeviceItem> currentDevices = devices.getValue();
        if (currentDevices == null) {
            currentDevices = new ArrayList<>();
        }
        
        List<BluetoothDeviceItem> updatedDevices = new ArrayList<>(currentDevices);
        updatedDevices.add(new BluetoothDeviceItem(device));
        devices.setValue(updatedDevices);
    }
    
    public void updateDeviceStatus(String deviceAddress, boolean isPaired, boolean isConnected) {
        List<BluetoothDeviceItem> currentDevices = devices.getValue();
        if (currentDevices == null) {
            return;
        }
        
        List<BluetoothDeviceItem> updatedDevices = new ArrayList<>();
        for (BluetoothDeviceItem deviceItem : currentDevices) {
            if (deviceItem.getAddress().equals(deviceAddress)) {
                deviceItem.setPaired(isPaired);
                deviceItem.setConnected(isConnected);
                // Also refresh the pairing status from the actual device to ensure consistency
                deviceItem.refreshPairingStatus();
            }
            updatedDevices.add(deviceItem);
        }
        devices.setValue(updatedDevices);
    }
    
    public void updateDeviceConnectionStatus(BluetoothDevice device, boolean isConnected) {
        if (device == null || device.getAddress() == null) {
            return;
        }
        
        List<BluetoothDeviceItem> currentDevices = devices.getValue();
        if (currentDevices == null) {
            return;
        }
        
        List<BluetoothDeviceItem> updatedDevices = new ArrayList<>();
        for (BluetoothDeviceItem deviceItem : currentDevices) {
            if (deviceItem.getAddress().equals(device.getAddress())) {
                deviceItem.setConnected(isConnected);
            } else if (isConnected) {
                // If connecting to a new device, disconnect all others
                deviceItem.setConnected(false);
            }
            updatedDevices.add(deviceItem);
        }
        devices.setValue(updatedDevices);
    }
    
    public void updateAllDevicesDisconnected() {
        List<BluetoothDeviceItem> currentDevices = devices.getValue();
        if (currentDevices == null) {
            return;
        }
        
        List<BluetoothDeviceItem> updatedDevices = new ArrayList<>();
        for (BluetoothDeviceItem deviceItem : currentDevices) {
            deviceItem.setConnected(false);
            updatedDevices.add(deviceItem);
        }
        devices.setValue(updatedDevices);
    }
    
    public void clearDevices() {
        devices.setValue(new ArrayList<>());
        deviceAddresses.clear();
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        deviceAddresses.clear();
    }
}