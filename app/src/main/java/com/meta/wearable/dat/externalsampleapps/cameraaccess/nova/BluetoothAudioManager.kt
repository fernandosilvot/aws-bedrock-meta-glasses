package com.meta.wearable.dat.externalsampleapps.cameraaccess.nova

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.util.Log

class BluetoothAudioManager(private val context: Context) {
    companion object {
        private const val TAG = "BluetoothAudioManager"
    }
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var wasRouted = false
    
    fun setupBluetoothMicrophone(): Boolean {
        return try {
            Log.d(TAG, "Setting up Bluetooth microphone for Meta Ray-Ban")
            
            val devices = audioManager.availableCommunicationDevices
            Log.d(TAG, "Available devices: ${devices.size}")
            
            var bluetoothDevice: AudioDeviceInfo? = null
            for (device in devices) {
                Log.d(TAG, "Device: ${device.productName}, Type: ${device.type}")
                if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    bluetoothDevice = device
                    break
                }
            }
            
            if (bluetoothDevice != null) {
                Log.d(TAG, "Found Meta Ray-Ban: ${bluetoothDevice.productName}")
                
                // Try MODE_IN_COMMUNICATION for input only
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                val success = audioManager.setCommunicationDevice(bluetoothDevice)
                
                if (success) {
                    wasRouted = true
                    Log.d(TAG, "✅ Using Meta Ray-Ban microphone (attempting to keep A2DP for output)")
                } else {
                    Log.e(TAG, "❌ Failed to route microphone")
                }
                
                success
            } else {
                Log.w(TAG, "⚠️ Meta Ray-Ban not found")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up Bluetooth microphone", e)
            false
        }
    }
    
    fun clearBluetoothAudio() {
        if (wasRouted) {
            try {
                audioManager.clearCommunicationDevice()
                audioManager.mode = AudioManager.MODE_NORMAL
                wasRouted = false
                Log.d(TAG, "Cleared Bluetooth audio routing")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing Bluetooth audio", e)
            }
        }
    }
}
