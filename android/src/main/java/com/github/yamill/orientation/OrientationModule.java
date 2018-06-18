package com.github.yamill.orientation;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.hardware.SensorEvent;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import main.java.com.github.yamill.orientation.DeviceOrientation;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class OrientationModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    final BroadcastReceiver receiver;
    final Sensor accelerometer;
    final Sensor magnetometer;
    final DeviceOrientation deviceOrientation;
    final SensorManager mSensorManager;

    private static final int ORIENTATION_0 = 0;
    private static final int ORIENTATION_270 = 1;
    private static final int ORIENTATION_180 = 2;
    private static final int ORIENTATION_90 = 3;

    public OrientationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        final ReactApplicationContext ctx = reactContext;

        mSensorManager = (SensorManager) getReactApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        deviceOrientation = new DeviceOrientation();

        mSensorManager.registerListener(deviceOrientation.getEventListener(), accelerometer,
                SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(deviceOrientation.getEventListener(), magnetometer,
                SensorManager.SENSOR_DELAY_UI);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Configuration newConfig = intent.getParcelableExtra("newConfig");
                Log.d("receiver", String.valueOf(newConfig.orientation));

                String orientationValue = newConfig.orientation == 1 ? "PORTRAIT" : "LANDSCAPE";

                WritableMap params = Arguments.createMap();
                params.putString("orientation", orientationValue);
                if (ctx.hasActiveCatalystInstance()) {
                    ctx.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("orientationDidChange",
                            params);
                }
            }
        };
        ctx.addLifecycleEventListener(this);
    }

    @Override
    public String getName() {
        return "Orientation";
    }

    @ReactMethod
    public void getOrientation(Callback callback) {
        final int orientationInt = getReactApplicationContext().getResources().getConfiguration().orientation;

        String orientation = this.getOrientationString(orientationInt);

        if (orientation == "null") {
            callback.invoke(orientationInt, null);
        } else {
            callback.invoke(null, orientation);
        }
    }

    @ReactMethod
    public void getSpecificOrientation(Callback callback) {
        WindowManager windowManager = ((WindowManager) getReactApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE));
        if (windowManager == null) {
            callback.invoke("No window manager", null);
        }
        Display display = windowManager.getDefaultDisplay();
        int screenOrientation = display.getRotation();
        String specifOrientationValue = getSpecificOrientationString(screenOrientation);
    }

    @ReactMethod
    public void getRealOrientation(Callback callback) {
        int orientation = deviceOrientation.getOrientation();
        String realOrientationValue = this.getOrientationString(orientation);

        callback.invoke(null, realOrientationValue);
    }

    @ReactMethod
    public void lockToPortrait() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }

    @ReactMethod
    public void lockToLandscape() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    @ReactMethod
    public void lockToLandscapeLeft() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    @ReactMethod
    public void lockToLandscapeRight() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

    @ReactMethod
    public void lockToPortraitUpsideDown() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
    }

    @ReactMethod
    public void unlockAllOrientations() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        this.onHostPause();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        this.onHostResume();
    }

    @Override
    public @Nullable Map<String, Object> getConstants() {
        HashMap<String, Object> constants = new HashMap<String, Object>();
        int orientationInt = getReactApplicationContext().getResources().getConfiguration().orientation;

        String orientation = this.getOrientationString(orientationInt);
        if (orientation == "null") {
            constants.put("initialOrientation", null);
        } else {
            constants.put("initialOrientation", orientation);
        }

        return constants;
    }

    private String getOrientationString(int orientation) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return "LANDSCAPE";
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return "PORTRAIT";
        } else if (orientation == Configuration.ORIENTATION_UNDEFINED) {
            return "UNKNOWN";
        } else {
            return "null";
        }
    }

    private String getSpecificOrientationString(int screenOrientation) {
        String specifOrientationValue;
        switch (screenOrientation) {
        case ORIENTATION_0: // Portrait
            specifOrientationValue = "PORTRAIT";
            break;
        case ORIENTATION_90: // Landscape right
            specifOrientationValue = "LANDSCAPE-RIGHT";
            break;
        case ORIENTATION_180: // Portrait upside down
            specifOrientationValue = "PORTRAITUPSIDEDOWN";
            break;
        case ORIENTATION_270: // Landscape left
            specifOrientationValue = "LANDSCAPE-LEFT";
            break;
        default:
            specifOrientationValue = "UNKNOWN";
            break;
        }
        return specifOrientationValue;
    }

    @Override
    public void onHostResume() {
        final Activity activity = getCurrentActivity();

        if (activity == null) {
            FLog.e(ReactConstants.TAG, "no activity to register receiver");
            return;
        }

        mSensorManager.registerListener(deviceOrientation.getEventListener(), accelerometer,
                SensorManager.SENSOR_DELAY_UI);
        activity.registerReceiver(receiver, new IntentFilter("onConfigurationChanged"));
    }

    @Override
    public void onHostPause() {
        final Activity activity = getCurrentActivity();

        if (activity == null)
            return;

        mSensorManager.unregisterListener(deviceOrientation.getEventListener());
        try {
            activity.unregisterReceiver(receiver);
        } catch (java.lang.IllegalArgumentException e) {
            FLog.e(ReactConstants.TAG, "receiver already unregistered", e);
        }
    }

    @Override
    public void onHostDestroy() {

    }
}
