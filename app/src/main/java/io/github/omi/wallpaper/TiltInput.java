package io.github.omi.wallpaper;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;

/** Gravity sensor with accelerometer fallback; scoped to visible surfaces. */
final class TiltInput implements SensorEventListener {
    private final SensorManager manager;
    private final Sensor sensor;
    private boolean registered, calibrated;
    private float baseX, baseY;
    float x, y;
    int rotation;

    TiltInput(Context context) {
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor gravity = manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensor = gravity != null ? gravity : manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    void start() {
        if (registered || sensor == null) return;
        calibrated = false;
        registered = manager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_GAME);
    }

    void stop() {
        if (manager != null) manager.unregisterListener(this);
        registered = false;
        calibrated = false;
        x = y = 0;
    }

    @Override public void onSensorChanged(SensorEvent event) {
        float sx = event.values[0], sy = event.values[1];
        switch (rotation) {
            case Surface.ROTATION_90: sx = -event.values[1]; sy = event.values[0]; break;
            case Surface.ROTATION_180: sx = -event.values[0]; sy = -event.values[1]; break;
            case Surface.ROTATION_270: sx = event.values[1]; sy = -event.values[0]; break;
            default: break;
        }
        if (!calibrated) { baseX=sx; baseY=sy; calibrated=true; }
        x += (SceneMath.clamp((sx-baseX)/4,-1,1)-x)*.12f;
        y += (SceneMath.clamp((sy-baseY)/4,-1,1)-y)*.12f;
    }
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy) {}
}
