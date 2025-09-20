package com.example.myapplication.ui.bluetooth;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.databinding.FragmentBluetoothScanBinding;

public class BluetoothScanFragment extends Fragment {

    private FragmentBluetoothScanBinding binding;
    private BluetoothScanViewModel viewModel;
    private BluetoothDeviceAdapter adapter;
    private BluetoothAdapter bluetoothAdapter;
    
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    
    private final BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    viewModel.addDevice(device);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                viewModel.setScanningState(false);
                updateScanButton();
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBluetoothScanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(BluetoothScanViewModel.class);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        setupRecyclerView();
        setupObservers();
        setupClickListeners();
        
        // Check if Bluetooth is supported
        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "Bluetooth not supported on this device", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        requireContext().registerReceiver(deviceFoundReceiver, filter);
    }
    
    private void setupRecyclerView() {
        adapter = new BluetoothDeviceAdapter();
        binding.recyclerViewDevices.setAdapter(adapter);
        binding.recyclerViewDevices.setLayoutManager(new LinearLayoutManager(getContext()));
    }
    
    private void setupObservers() {
        viewModel.getDevices().observe(getViewLifecycleOwner(), devices -> {
            adapter.submitList(devices);
            binding.textNoDevices.setVisibility(devices.isEmpty() ? View.VISIBLE : View.GONE);
        });
        
        viewModel.isScanning().observe(getViewLifecycleOwner(), isScanning -> {
            binding.progressBar.setVisibility(isScanning ? View.VISIBLE : View.GONE);
            updateScanButton();
        });
    }
    
    private void setupClickListeners() {
        binding.buttonScan.setOnClickListener(v -> {
            if (viewModel.isScanning().getValue() == Boolean.TRUE) {
                stopScanning();
            } else {
                startScanning();
            }
        });
        
        binding.buttonClear.setOnClickListener(v -> viewModel.clearDevices());
    }
    
    private void startScanning() {
        if (!hasRequiredPermissions()) {
            requestPermissions();
            return;
        }
        
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }
        
        // Cancel any ongoing discovery
        if (bluetoothAdapter.isDiscovering()) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.cancelDiscovery();
            }
        }
        
        // Start discovery
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            boolean started = bluetoothAdapter.startDiscovery();
            if (started) {
                viewModel.setScanningState(true);
                Toast.makeText(getContext(), "Scanning for devices...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to start scanning", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void stopScanning() {
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.cancelDiscovery();
            }
        }
        viewModel.setScanningState(false);
        Toast.makeText(getContext(), "Scanning stopped", Toast.LENGTH_SHORT).show();
    }
    
    private void updateScanButton() {
        boolean isScanning = viewModel.isScanning().getValue() == Boolean.TRUE;
        binding.buttonScan.setText(isScanning ? "Stop Scan" : "Start Scan");
        binding.buttonScan.setEnabled(bluetoothAdapter != null);
    }
    
    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                   ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                   ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                   ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                   ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_PERMISSIONS);
        } else {
            requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            }, REQUEST_PERMISSIONS);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Toast.makeText(getContext(), "Permissions granted. You can now scan for devices.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Permissions required for Bluetooth scanning", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == getActivity().RESULT_OK) {
                Toast.makeText(getContext(), "Bluetooth enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Bluetooth is required for scanning", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Stop scanning if in progress
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.cancelDiscovery();
            }
        }
        
        // Unregister broadcast receiver
        try {
            requireContext().unregisterReceiver(deviceFoundReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
        
        binding = null;
    }
}