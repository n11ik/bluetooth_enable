package com.example.bluetooth_enable;

import android.app.Activity;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class BluetoothEnablePlugin implements FlutterPlugin, ActivityAware, MethodCallHandler {
    private static final String TAG = "BluetoothEnablePlugin";
    private Activity activity;
    private MethodChannel channel;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private Result pendingResult;

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;

    /** Plugin registration. */
    @Override
    public void onAttachedToEngine(FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "bluetooth_enable");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        this.activity = null;
        this.mBluetoothAdapter = null;
        this.mBluetoothManager = null;
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (mBluetoothAdapter == null && !"isAvailable".equals(call.method)) {
            result.error("bluetooth_unavailable", "the device does not have bluetooth", null);
            return;
        }

        ActivityCompat.requestPermissions(this.activity,
                new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                REQUEST_ENABLE_BLUETOOTH);

        switch (call.method) {
            case "enableBluetooth":
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
                pendingResult = result;
                break;
            case "customEnable":
                try {
                    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (!mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.disable();
                        Thread.sleep(500); // Thread.sleep() for Bluetooth enable delay
                        mBluetoothAdapter.enable();
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "customEnable", e);
                }
                result.success("true");
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (pendingResult == null) {
                Log.d(TAG, "onActivityResult: problem: pendingResult is null");
            } else {
                try {
                    if (resultCode == Activity.RESULT_OK) {
                        Log.d(TAG, "onActivityResult: User enabled Bluetooth");
                        pendingResult.success("true");
                    } else {
                        Log.d(TAG, "onActivityResult: User did NOT enable Bluetooth");
                        pendingResult.success("false");
                    }
                } catch (IllegalStateException | NullPointerException e) {
                    Log.d(TAG, "onActivityResult REQUEST_ENABLE_BLUETOOTH", e);
                }
            }
        }
        return false;
    }

    /* ActivityAware implementation */
    @Override
    public void onAttachedToActivity(ActivityPluginBinding activityPluginBinding) {
        this.activity = activityPluginBinding.getActivity();
        this.mBluetoothManager = (BluetoothManager) this.activity.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mBluetoothAdapter = mBluetoothManager.getAdapter();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding activityPluginBinding) {
        this.activity = activityPluginBinding.getActivity();
        this.mBluetoothManager = (BluetoothManager) this.activity.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mBluetoothAdapter = mBluetoothManager.getAdapter();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        releaseResources();
    }

    @Override
    public void onDetachedFromActivity() {
        releaseResources();
    }

    private void releaseResources() {
        this.activity = null;
        this.mBluetoothAdapter = null;
        this.mBluetoothManager = null;
    }
}
