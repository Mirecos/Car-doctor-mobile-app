package com.example.myapplication.ui.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Service class to handle Bluetooth RFCOMM connections to the Python server
 */
public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionService";
    
    // SPP UUID - same as used in Python server
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Thread connectionThread;
    private boolean isConnected = false;
    private Context context;
    private ConnectionListener listener;
    private Handler mainHandler;
    
    public interface ConnectionListener {
        void onConnected(BluetoothDevice device);
        void onDisconnected();
        void onConnectionFailed(String error);
        void onMessageReceived(String message);
        void onMessageSent(String message);
    }
    
    public BluetoothConnectionService(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public void setConnectionListener(ConnectionListener listener) {
        this.listener = listener;
    }
    
    /**
     * Connect to a Bluetooth device using RFCOMM channel 4 (for Python server)
     */
    public void connect(BluetoothDevice device) {
        if (isConnected) {
            disconnect();
        }
        
        connectionThread = new Thread(() -> {
            try {
                // Check permissions
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
                    != PackageManager.PERMISSION_GRANTED) {
                    notifyConnectionFailed("Bluetooth permission not granted");
                    return;
                }
                
                Log.d(TAG, "Attempting to connect to device: " + device.getName());
                
                // Cancel discovery to improve connection performance
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter != null && adapter.isDiscovering()) {
                    adapter.cancelDiscovery();
                }
                
                // Try multiple connection methods
                boolean connected = false;
                
                // Method 1: Try to connect to RFCOMM channel 4 directly using reflection (for Python server)
                try {
                    Log.d(TAG, "Trying to connect to RFCOMM channel 4 using reflection...");
                    socket = (BluetoothSocket) device.getClass()
                        .getMethod("createRfcommSocket", int.class)
                        .invoke(device, 4);
                    socket.connect();
                    connected = true;
                    Log.d(TAG, "Connected using RFCOMM channel 4");
                } catch (Exception e) {
                    Log.w(TAG, "Failed to connect to channel 4: " + e.getMessage());
                    if (socket != null) {
                        try { socket.close(); } catch (Exception ignored) {}
                        socket = null;
                    }
                }
                
                // Small delay before trying next method
                if (!connected) {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
                
                // Method 2: Fallback to SPP UUID if channel 4 failed
                if (!connected) {
                    try {
                        Log.d(TAG, "Trying to connect using SPP UUID...");
                        socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                        socket.connect();
                        connected = true;
                        Log.d(TAG, "Connected using SPP UUID");
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to connect using SPP UUID: " + e.getMessage());
                        if (socket != null) {
                            try { socket.close(); } catch (Exception ignored) {}
                            socket = null;
                        }
                    }
                }
                
                // Small delay before trying next method
                if (!connected) {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
                
                // Method 3: Try creating an insecure RFCOMM socket
                if (!connected) {
                    try {
                        Log.d(TAG, "Trying insecure RFCOMM socket...");
                        socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                        socket.connect();
                        connected = true;
                        Log.d(TAG, "Connected using insecure RFCOMM socket");
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to connect using insecure socket: " + e.getMessage());
                        if (socket != null) {
                            try { socket.close(); } catch (Exception ignored) {}
                            socket = null;
                        }
                    }
                }
                
                if (!connected) {
                    throw new IOException("All connection methods failed");
                }
                
                // Get input and output streams
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                
                isConnected = true;
                Log.d(TAG, "Successfully connected to device: " + device.getName());
                
                // Notify connection success on main thread
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onConnected(device);
                    }
                });
                
                // Start listening for incoming messages
                listenForMessages();
                
            } catch (IOException e) {
                Log.e(TAG, "Connection failed: " + e.getMessage());
                cleanup();
                notifyConnectionFailed("Connection failed: " + e.getMessage());
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception: " + e.getMessage());
                cleanup();
                notifyConnectionFailed("Permission denied: " + e.getMessage());
            }
        });
        
        connectionThread.start();
    }
    
    /**
     * Listen for incoming messages from the server
     */
    private void listenForMessages() {
        byte[] buffer = new byte[1024];
        
        while (isConnected && socket != null && socket.isConnected()) {
            try {
                int bytesRead = inputStream.read(buffer);
                if (bytesRead > 0) {
                    String message = new String(buffer, 0, bytesRead, "UTF-8").trim();
                    Log.d(TAG, "Received message: " + message);
                    
                    // Notify message received on main thread
                    mainHandler.post(() -> {
                        if (listener != null) {
                            listener.onMessageReceived(message);
                        }
                    });
                }
            } catch (IOException e) {
                if (isConnected) {
                    Log.e(TAG, "Error reading message: " + e.getMessage());
                    disconnect();
                }
                break;
            }
        }
    }
    
    /**
     * Send a message to the connected device
     */
    public void sendMessage(String message) {
        if (!isConnected || outputStream == null) {
            Log.w(TAG, "Cannot send message - not connected");
            return;
        }
        
        new Thread(() -> {
            try {
                String messageWithNewline = message + "\n";
                outputStream.write(messageWithNewline.getBytes("UTF-8"));
                outputStream.flush();
                
                Log.d(TAG, "Sent message: " + message);
                
                // Notify message sent on main thread
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onMessageSent(message);
                    }
                });
                
            } catch (IOException e) {
                Log.e(TAG, "Error sending message: " + e.getMessage());
                disconnect();
            }
        }).start();
    }
    
    /**
     * Disconnect from the current device
     */
    public void disconnect() {
        isConnected = false;
        cleanup();
        
        // Notify disconnection on main thread
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onDisconnected();
            }
        });
    }
    
    /**
     * Clean up resources
     */
    private void cleanup() {
        isConnected = false;
        
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing input stream: " + e.getMessage());
        }
        
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing output stream: " + e.getMessage());
        }
        
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket: " + e.getMessage());
        }
    }
    
    /**
     * Check if currently connected
     */
    public boolean isConnected() {
        return isConnected && socket != null && socket.isConnected();
    }
    
    /**
     * Notify connection failure on main thread
     */
    private void notifyConnectionFailed(String error) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onConnectionFailed(error);
            }
        });
    }
    
    /**
     * Clean up when service is destroyed
     */
    public void destroy() {
        disconnect();
        if (connectionThread != null && connectionThread.isAlive()) {
            connectionThread.interrupt();
        }
    }
}