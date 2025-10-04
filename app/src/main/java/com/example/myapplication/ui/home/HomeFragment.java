package com.example.myapplication.ui.home;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.databinding.FragmentHomeBinding;
import com.example.myapplication.ui.bluetooth.BluetoothConnectionService;
import com.example.myapplication.ui.bluetooth.BluetoothDeviceAdapter;
import com.example.myapplication.ui.bluetooth.BluetoothDeviceItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment implements BluetoothDeviceAdapter.OnDeviceActionListener {

    private FragmentHomeBinding binding;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothConnectionService connectionService;
    private BluetoothDeviceAdapter deviceAdapter;
    private List<BluetoothDeviceItem> discoveredDevices;
    private BluetoothDevice selectedDevice;
    private boolean isConnected = false;
    private boolean isScanning = false;
    
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    
    private final BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    addDiscoveredDevice(device);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                isScanning = false;
                updateScanButton();
                appendToMessageLog("Device scan completed.");
            }
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        
        initializeBluetooth();
        setupUI();
        setupRecyclerView();
        setupClickListeners();
        
        return root;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Register broadcast receiver for device discovery
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        requireContext().registerReceiver(deviceFoundReceiver, filter);
    }
    
    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        discoveredDevices = new ArrayList<>();
        
        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "Bluetooth not supported on this device", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Initialize connection service
        connectionService = new BluetoothConnectionService(getContext());
        setupConnectionService();
    }
    
    private void setupConnectionService() {
        connectionService.setConnectionListener(new BluetoothConnectionService.ConnectionListener() {
            @Override
            public void onConnected(BluetoothDevice device) {
                isConnected = true;
                selectedDevice = device;
                updateConnectionStatus("Connected to " + getDeviceName(device));
                updateUIState();
                appendToMessageLog("Connected to " + getDeviceName(device));
            }
            
            @Override
            public void onDisconnected() {
                isConnected = false;
                selectedDevice = null;
                updateConnectionStatus("Disconnected");
                updateUIState();
                appendToMessageLog("Disconnected from device");
            }
            
            @Override
            public void onConnectionFailed(String error) {
                isConnected = false;
                updateConnectionStatus("Connection failed: " + error);
                updateUIState();
                appendToMessageLog("Connection failed: " + error);
            }
            
            @Override
            public void onMessageReceived(String message) {
                appendToMessageLog("Received: " + message);
            }
            
            @Override
            public void onMessageSent(String message) {
                appendToMessageLog("Sent: " + message);
            }
        });
    }
    
    private void setupUI() {
        updateConnectionStatus("Not connected");
        updateUIState();
    }
    
    private void setupRecyclerView() {
        deviceAdapter = new BluetoothDeviceAdapter();
        deviceAdapter.setOnDeviceActionListener(this);
        binding.recyclerViewDevices.setAdapter(deviceAdapter);
        binding.recyclerViewDevices.setLayoutManager(new LinearLayoutManager(getContext()));
    }
    
    private void setupClickListeners() {
        binding.buttonScanDevices.setOnClickListener(v -> {
            if (isScanning) {
                stopDeviceDiscovery();
            } else {
                startDeviceDiscovery();
            }
        });
        
        binding.buttonConnectDisconnect.setOnClickListener(v -> {
            if (isConnected) {
                disconnectFromDevice();
            } else {
                // Connect button is enabled only when a device is selected
                connectToSelectedDevice();
            }
        });
        
        binding.buttonSendMessage.setOnClickListener(v -> sendMessage());
    }
    
    private void startDeviceDiscovery() {
        if (!hasRequiredPermissions()) {
            requestPermissions();
            return;
        }
        
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }
        
        // Clear previous results
        discoveredDevices.clear();
        deviceAdapter.submitList(new ArrayList<>(discoveredDevices));
        
        // Add paired devices first
        addPairedDevices();
        
        // Start discovery
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        
        boolean discoveryStarted = bluetoothAdapter.startDiscovery();
        if (discoveryStarted) {
            isScanning = true;
            updateScanButton();
            appendToMessageLog("Scanning for devices...");
        } else {
            Toast.makeText(getContext(), "Failed to start device discovery", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopDeviceDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        isScanning = false;
        updateScanButton();
        appendToMessageLog("Device scan stopped.");
    }
    
    private void addPairedDevices() {
        if (!hasRequiredPermissions()) return;
        
        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                addDiscoveredDevice(device);
            }
        } catch (SecurityException e) {
            // Handle permission error
        }
    }
    
    private void addDiscoveredDevice(BluetoothDevice device) {
        // Check if device already in list
        for (BluetoothDeviceItem item : discoveredDevices) {
            if (item.getAddress().equals(device.getAddress())) {
                return; // Already added
            }
        }
        
        BluetoothDeviceItem deviceItem = new BluetoothDeviceItem(device);
        discoveredDevices.add(deviceItem);
        deviceAdapter.submitList(new ArrayList<>(discoveredDevices));
    }
    
    private void connectToSelectedDevice() {
        if (selectedDevice != null && connectionService != null) {
            updateConnectionStatus("Connecting to " + getDeviceName(selectedDevice) + "...");
            connectionService.connect(selectedDevice);
        }
    }
    
    private void disconnectFromDevice() {
        if (connectionService != null) {
            connectionService.disconnect();
        }
    }
    
    private void sendMessage() {
        String message = binding.editMessage.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (connectionService != null && isConnected) {
            connectionService.sendMessage(message);
            binding.editMessage.setText(""); // Clear input
        } else {
            Toast.makeText(getContext(), "Not connected to any device", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateScanButton() {
        binding.buttonScanDevices.setText(isScanning ? "Stop Scan" : "Scan Devices");
    }
    
    private void updateConnectionStatus(String status) {
        binding.textConnectionStatus.setText("Status: " + status);
    }
    
    private void updateUIState() {
        binding.buttonConnectDisconnect.setText(isConnected ? "Disconnect" : "Connect");
        binding.buttonConnectDisconnect.setEnabled(isConnected || selectedDevice != null);
        binding.editMessage.setEnabled(isConnected);
        binding.buttonSendMessage.setEnabled(isConnected);
    }
    
    private void appendToMessageLog(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String logEntry = "[" + timestamp + "] " + message;
        
        String currentLog = binding.textMessageLog.getText().toString();
        if (currentLog.equals("Messages will appear here...")) {
            currentLog = "";
        }
        
        String newLog = currentLog + "\n" + logEntry;
        binding.textMessageLog.setText(newLog);
        
        // Scroll to bottom of the ScrollView
        binding.scrollMessageLog.post(() -> {
            binding.scrollMessageLog.fullScroll(android.view.View.FOCUS_DOWN);
        });
    }
    
    private String getDeviceName(BluetoothDevice device) {
        try {
            if (hasRequiredPermissions()) {
                String name = device.getName();
                return name != null ? name : "Unknown Device";
            }
        } catch (SecurityException e) {
            // Handle permission error
        }
        return "Unknown Device";
    }
    
    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                   ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        
        requestPermissions(permissions.toArray(new String[0]), REQUEST_PERMISSIONS);
    }
    
    @Override
    public void onPairUnpairClick(BluetoothDeviceItem deviceItem) {
        if (!hasRequiredPermissions()) {
            requestPermissions();
            return;
        }
        
        BluetoothDevice device = deviceItem.getDevice();
        if (device == null) return;
        
        try {
            if (deviceItem.isPaired()) {
                // Unpair device using reflection
                device.getClass().getMethod("removeBond").invoke(device);
                String deviceName = getDeviceName(device);
                Toast.makeText(getContext(), "Unpairing " + deviceName, Toast.LENGTH_SHORT).show();
                appendToMessageLog("Unpairing " + deviceName);
            } else {
                // Pair device
                boolean pairingStarted = device.createBond();
                if (pairingStarted) {
                    String deviceName = getDeviceName(device);
                    Toast.makeText(getContext(), "Pairing with " + deviceName, Toast.LENGTH_SHORT).show();
                    appendToMessageLog("Pairing with " + deviceName);
                } else {
                    Toast.makeText(getContext(), "Failed to start pairing", Toast.LENGTH_SHORT).show();
                    appendToMessageLog("Failed to start pairing");
                }
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Pairing operation failed", Toast.LENGTH_SHORT).show();
            appendToMessageLog("Pairing operation failed: " + e.getMessage());
        }
    }
    
    @Override
    public void onConnectClick(BluetoothDeviceItem deviceItem) {
        selectedDevice = deviceItem.getDevice();
        updateUIState();
        
        String deviceName = getDeviceName(selectedDevice);
        Toast.makeText(getContext(), "Selected: " + deviceName, Toast.LENGTH_SHORT).show();
        appendToMessageLog("Selected device: " + deviceName + " (" + deviceItem.getAddress() + ")");
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Stop discovery if running
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering() && hasRequiredPermissions()) {
            bluetoothAdapter.cancelDiscovery();
        }
        
        // Unregister broadcast receiver
        try {
            requireContext().unregisterReceiver(deviceFoundReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
        
        // Clean up connection service
        if (connectionService != null) {
            connectionService.destroy();
        }
        
        binding = null;
    }
}