package io.github.omi.wallpaper;

import android.content.Context;
import android.graphics.Canvas;
import android.os.PowerManager;
import android.view.MotionEvent;
import android.view.View;

final class PreviewView extends View {
    private final SceneSettings settings;
    private final SceneRenderer renderer;
    private final TiltInput tilt;
    private final PowerManager power;
    private boolean active,playing=true;
    private final Runnable tick=this::invalidate;

    public PreviewView(Context context) { this(context,new SceneSettings(context)); }

    PreviewView(Context context,SceneSettings settings) {
        super(context);
        this.settings=settings;
        renderer=new SceneRenderer(context,settings);
        tilt=new TiltInput(context);
        power=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
        setContentDescription("OMI 互动壁纸预览，轻触唤起光点，拖动改变视差");
        setClickable(true);
    }

    void resume() { active=true; AppDiagnostics.previewState(true); refresh(); }
    void pause() { active=false; AppDiagnostics.previewState(false); removeCallbacks(tick); tilt.stop(); renderer.pause(); }
    void release() { pause(); renderer.release(); }
    void setPlaying(boolean value) { playing=value; if(!value) {removeCallbacks(tick); tilt.stop(); renderer.pause();} else refresh(); }
    void refresh() {
        settings.reload();
        int layer=settings.software ? View.LAYER_TYPE_SOFTWARE : View.LAYER_TYPE_NONE;
        if(getLayerType()!=layer) setLayerType(layer,null);
        if(getDisplay()!=null) tilt.rotation=getDisplay().getRotation();
        if(active && playing && settings.tilt) tilt.start(); else tilt.stop();
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        renderer.setTilt(tilt.x,tilt.y);
        renderer.draw(canvas,getWidth(),getHeight());
        AppDiagnostics.previewFrame();
        removeCallbacks(tick);
        if(active && playing && renderer.animated()) {
            int fps=power!=null && power.isPowerSaveMode() ? 15 : settings.fps;
            postDelayed(tick,1000/fps);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        AppDiagnostics.previewTouch();
        if(playing) {
            getParent().requestDisallowInterceptTouchEvent(event.getActionMasked()!=MotionEvent.ACTION_UP && event.getActionMasked()!=MotionEvent.ACTION_CANCEL);
            renderer.touch(event); invalidate();
        }
        if(event.getActionMasked()==MotionEvent.ACTION_UP) performClick();
        return true;
    }

    @Override public boolean performClick() { super.performClick(); return true; }
}
