package io.github.omi.wallpaper;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public final class OmiWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine() { return new SceneEngine(); }

    private final class SceneEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
        private final Handler handler=new Handler(Looper.getMainLooper());
        private final SceneSettings settings=new SceneSettings(OmiWallpaperService.this);
        private final TiltInput tilt=new TiltInput(OmiWallpaperService.this);
        private final PowerManager power=(PowerManager)getSystemService(POWER_SERVICE);
        private SceneRenderer renderer;
        private boolean visible,surfaceReady,destroyed,hardwareCanvas=true;
        private boolean surfaceErrorRecorded;
        private int diagnosticId;
        private final Runnable frame=this::draw;

        @Override public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            diagnosticId=AppDiagnostics.engineCreated(isPreview());
            renderer=new SceneRenderer(OmiWallpaperService.this,settings);
            setTouchEventsEnabled(true);
            setOffsetNotificationsEnabled(true);
            settings.prefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override public void onVisibilityChanged(boolean isVisible) {
            visible=isVisible;
            AppDiagnostics.engineVisible(diagnosticId,isVisible);
            updateSensors();
            handler.removeCallbacks(frame);
            if(visible) draw(); else renderer.pause();
        }

        private void updateSensors() {
            if(visible && surfaceReady && settings.tilt && !destroyed) tilt.start(); else tilt.stop();
        }

        @Override public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            surfaceReady=true;
            AppDiagnostics.event("engine#"+diagnosticId+" surface_created");
            hardwareCanvas=true;
            updateSensors(); draw();
        }

        @Override public void onSurfaceChanged(SurfaceHolder holder,int format,int width,int height) {
            super.onSurfaceChanged(holder,format,width,height);
            AppDiagnostics.event("engine#"+diagnosticId+" surface="+width+"x"+height);
            if(android.os.Build.VERSION.SDK_INT>=30 && getDisplayContext().getDisplay()!=null)
                tilt.rotation=getDisplayContext().getDisplay().getRotation();
            draw();
        }

        private void draw() {
            handler.removeCallbacks(frame);
            if(destroyed || !surfaceReady || !visible) return;
            SurfaceHolder holder=getSurfaceHolder();
            if(!holder.getSurface().isValid()) return;
            Canvas canvas=null;
            try {
                if(hardwareCanvas && !settings.software) {
                    try { canvas=holder.lockHardwareCanvas(); }
                    catch (IllegalStateException | IllegalArgumentException unsupported) { hardwareCanvas=false; }
                }
                if(canvas==null) canvas=holder.lockCanvas();
                if(canvas!=null) {
                    renderer.setTilt(tilt.x,tilt.y);
                    renderer.draw(canvas,canvas.getWidth(),canvas.getHeight());
                }
            } catch (IllegalStateException | IllegalArgumentException surfaceGone) {
                if(!surfaceErrorRecorded) { AppDiagnostics.event("wallpaper_draw_error="+surfaceGone); surfaceErrorRecorded=true; }
                // A launcher may replace the surface between validation and lock.
            } finally {
                if(canvas!=null) {
                    try {
                        boolean hardware=canvas.isHardwareAccelerated();
                        holder.unlockCanvasAndPost(canvas);
                        AppDiagnostics.enginePosted(diagnosticId,hardware,tilt.x,tilt.y);
                    }
                    catch (IllegalStateException | IllegalArgumentException surfaceGone) { /* surface replaced */ }
                }
            }
            if(visible && surfaceReady && !destroyed && renderer.animated()) {
                int fps=power!=null && power.isPowerSaveMode() ? 15 : settings.fps;
                handler.postDelayed(frame,1000/fps);
            }
        }

        @Override public void onTouchEvent(MotionEvent event) {
            AppDiagnostics.engineTouch(diagnosticId,event.getActionMasked(),event.getX(),event.getY());
            renderer.touch(event);
            if(visible) draw();
        }

        @Override public Bundle onCommand(String action,int x,int y,int z,Bundle extras,boolean resultRequested) {
            if(WallpaperManager.COMMAND_TAP.equals(action)) { AppDiagnostics.engineCommand(diagnosticId); renderer.commandTap(x,y); draw(); }
            return null;
        }

        @Override public void onOffsetsChanged(float xOffset,float yOffset,float xStep,float yStep,int xPixels,int yPixels) {
            AppDiagnostics.engineOffset(diagnosticId);
            if(renderer!=null) { renderer.setLauncherOffset(xOffset); if(visible) draw(); }
        }

        @Override public WallpaperColors onComputeColors() {
            return new WallpaperColors(Color.valueOf(0xffaaba9b),Color.valueOf(0xff526966),Color.valueOf(0xfffafbf7));
        }

        @Override public void onSharedPreferenceChanged(SharedPreferences preferences,String key) {
            settings.reload(); updateSensors(); draw();
        }

        @Override public void onSurfaceDestroyed(SurfaceHolder holder) {
            AppDiagnostics.event("engine#"+diagnosticId+" surface_destroyed");
            surfaceReady=false;
            handler.removeCallbacks(frame);
            tilt.stop(); renderer.pause();
            super.onSurfaceDestroyed(holder);
        }

        @Override public void onDestroy() {
            AppDiagnostics.engineDestroyed(diagnosticId);
            destroyed=true; visible=false;
            handler.removeCallbacksAndMessages(null);
            tilt.stop();
            settings.prefs.unregisterOnSharedPreferenceChangeListener(this);
            if(renderer!=null) renderer.release();
            super.onDestroy();
        }
    }
}
