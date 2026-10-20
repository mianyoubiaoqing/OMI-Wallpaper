package io.github.omi.wallpaper;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.FrameLayout;

/** An app-owned surface lets the user compare interactions independently of the launcher. */
public final class FullPreviewActivity extends Activity {
    private PreviewView preview;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout content=new FrameLayout(this);
        preview=new PreviewView(this);
        content.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        Button back=new Button(this); back.setText("返回设置"); back.setTextColor(Color.WHITE);
        back.setBackgroundColor(0xaa183829); back.setOnClickListener(view->finish());
        int dp=Math.round(getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams position=new FrameLayout.LayoutParams(112*dp,48*dp,Gravity.BOTTOM|Gravity.END);
        position.setMargins(20*dp,20*dp,20*dp,40*dp); content.addView(back,position);
        setContentView(content);
        if(android.os.Build.VERSION.SDK_INT>=30) {
            WindowInsetsController bars=getWindow().getInsetsController();
            if(bars!=null) { bars.hide(WindowInsets.Type.systemBars()); bars.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE); }
        }
    }
    @Override protected void onResume() { super.onResume(); preview.resume(); }
    @Override protected void onPause() { preview.pause(); super.onPause(); }
    @Override protected void onDestroy() { preview.release(); super.onDestroy(); }
}
