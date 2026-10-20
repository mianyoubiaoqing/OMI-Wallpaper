package io.github.omi.wallpaper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int BG=0xff141d19, CARD=0xff202d25, INK=0xffeef3e8, MUTED=0xffa9b8a8, ACCENT=0xffc7dfb0;
    private SceneSettings settings;
    private PreviewView preview;
    private LinearLayout body;
    private WallpaperApplyController wallpaperApply;
    private boolean playing=true;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        settings=new SceneSettings(this);
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(BG); scroll.setFillViewport(true);
        body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(24),dp(20),dp(24),dp(32));
        scroll.addView(body);
        setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view,insets)-> {
            int top,bottom,left,right;
            if(android.os.Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                top=safe.top; bottom=safe.bottom; left=safe.left; right=safe.right;
            } else {
                top=insets.getSystemWindowInsetTop(); bottom=insets.getSystemWindowInsetBottom();
                left=insets.getSystemWindowInsetLeft(); right=insets.getSystemWindowInsetRight();
            }
            view.setPadding(left,top,right,bottom);
            return insets;
        });

        TextView eyebrow=text("O M I    /    L I V E  W A L L P A P E R",10,ACCENT);
        body.addView(eyebrow);
        TextView title=text("OMI Wallpaper",34,INK); title.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        add(title,8,0);
        add(text("让风停在你的指尖。",14,MUTED),5,20);

        FrameLayout frame=new FrameLayout(this);
        GradientDrawable round=background(0xffeef1e9,22);
        frame.setBackground(round); frame.setClipToOutline(true);
        preview=new PreviewView(this,settings);
        frame.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        // Use the device's actual portrait aspect, including the wallpaper's system-bar area.
        int screenW=getResources().getDisplayMetrics().widthPixels;
        int screenH=getResources().getDisplayMetrics().heightPixels;
        float aspect=(float)Math.max(screenW,screenH)/Math.min(screenW,screenH);
        int previewHeight=dp(360);
        int previewWidth=Math.min(screenW-dp(48),Math.round(previewHeight/aspect));
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(previewWidth,previewHeight);
        fp.gravity=Gravity.CENTER_HORIZONTAL;
        body.addView(frame,fp);
        LinearLayout toolbar=new LinearLayout(this); toolbar.setGravity(Gravity.CENTER_VERTICAL);
        TextView hint=text("轻触 · 流光     拖动 · 微风",12,MUTED);
        toolbar.addView(hint,new LinearLayout.LayoutParams(0,dp(44),1)); hint.setGravity(Gravity.CENTER_VERTICAL);
        Button pause=button("暂停",false);
        pause.setOnClickListener(v->{playing=!playing; preview.setPlaying(playing); pause.setText(playing ? "暂停" : "播放");});
        toolbar.addView(pause,new LinearLayout.LayoutParams(dp(74),dp(40)));
        add(toolbar,10,14);
        Button fullPreview=button("全屏互动预览",false);
        fullPreview.setOnClickListener(v->startActivity(new Intent(this,FullPreviewActivity.class)));
        add(fullPreview,0,10);

        Button apply=button("设为动态壁纸  ↗",true); apply.setTextSize(16);
        apply.setOnClickListener(v->wallpaperApply.openDefault());
        body.addView(apply,new LinearLayout.LayoutParams(-1,dp(54)));
        TextView note=text("在系统预览页确认应用",11,MUTED); note.setGravity(Gravity.CENTER);
        add(note,8,10);
        wallpaperApply=new WallpaperApplyController(this,note,state);
        Button compatible=button("兼容设置入口",false);
        compatible.setOnClickListener(v->wallpaperApply.openCompatible()); add(compatible,0,6);
        Button alternatives=button("其他设置方式 / 诊断",false);
        alternatives.setOnClickListener(v->wallpaperApply.showAlternatives()); add(alternatives,0,16);
        Button diagnostics=button("诊断与日志 · 导出 TXT",false);
        diagnostics.setOnClickListener(v->wallpaperApply.showDiagnostics()); add(diagnostics,0,8);

        section("风与光");
        toggle("人物轻摆","头部、手臂与手掌持续活动，10 秒循环","character",true);
        slider("人物动作幅度","poseStrength",80,100,"静止","鲜明");
        slider("手部摆幅","handStrength",100,100,"轻柔","明显");
        toggle("发丝轻风","发根固定，发梢错峰轻摆","motion",true);
        toggle("眨眼与表情","连续眨眼；待机闭嘴微笑，轻触张嘴 1.8 秒","expressions",true);
        toggle("指尖流光","点按回应与涟漪，拖动带起发丝","touch",true);
        toggle("随手机倾斜","轻轻转动手机，感受视差","tilt",true);
        toggle("漂浮微光","稀疏的青绿与浅金光点","particles",true);
        slider("发丝摆幅","strength",45,100,"轻柔","鲜明");
        section("画面构图");
        slider("左右取景","framing",18,100,"画面左侧","画面右侧");
        slider("画面放大","zoom",0,30,"原始","放大");
        section("播放");
        toggle("流畅模式 · 60 帧","默认 30 帧；省电模式自动降为 15 帧","smooth",false);
        toggle("兼容绘制","仅画面异常时开启；软件画布可能更耗电","software",false);
        add(text("离开桌面或息屏后自动暂停。\n互动效果是否传入壁纸，取决于你的桌面与锁屏。",12,MUTED),14,20);
        Button help=button("安装与使用说明",false);
        help.setOnClickListener(v->showHelp()); add(help,0,12);
        TextView credit=text("OMI Wallpaper · MIT\n原创程序绘制演示 · v"+BuildConfig.VERSION_NAME,11,MUTED);
        credit.setGravity(Gravity.CENTER); add(credit,10,0);
    }

    private void showHelp() {
        new AlertDialog.Builder(this).setTitle("把一缕风带到桌面")
            .setMessage("1. 点击「设为动态壁纸」，在系统预览页确认。\n\n2. 若没有直接打开，进入系统设置 → 壁纸 → 动态壁纸，选择「OMI Wallpaper」。不同品牌入口名称可能不同。\n\n3. 可选主屏幕或主屏幕与锁屏；是否支持单独锁屏，由系统决定。\n\n4. 在桌面空白处轻触和滑动，或轻微倾斜手机。图标、桌面手势或锁屏可能拦截触摸；应用内预览可随时体验完整互动。\n\n5. 人物以头、手腕和前臂为支点持续轻摆，肩背保持形状稳定；发丝使用限定区域的网格动画；眼睑连续移动；平时闭嘴微笑，短点按后张嘴 1.8 秒再恢复；没有角色对话、换装或完整 Live2D 骨骼。应用不申请联网、存储、麦克风或相机权限。")
            .setPositiveButton("知道了",null).show();
    }

    private void section(String title) {
        TextView view=text(title,17,INK); view.setTypeface(null,Typeface.BOLD); add(view,20,12);
    }

    private void toggle(String title,String subtitle,String key,boolean def) {
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(16),dp(13),dp(12),dp(13)); row.setBackground(background(CARD,14));
        LinearLayout labels=new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title,14,INK));
        TextView sub=text(subtitle,11,MUTED); sub.setPadding(0,dp(5),dp(8),0); labels.addView(sub);
        row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        Switch control=new Switch(this); control.setContentDescription(title); control.setChecked(settings.prefs.getBoolean(key,def));
        control.setOnCheckedChangeListener((button,checked)->{settings.prefs.edit().putBoolean(key,checked).apply(); preview.refresh();});
        row.addView(control); row.setOnClickListener(v->control.toggle()); add(row,0,7);
    }

    private void slider(String title,String key,int def,int max,String left,String right) {
        LinearLayout panel=new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(background(CARD,14)); panel.setPadding(dp(16),dp(16),dp(16),dp(10));
        int initial=settings.prefs.getInt(key,def);
        TextView label=text(getString(R.string.slider_value,title,initial),13,INK); panel.addView(label);
        SeekBar seek=new SeekBar(this); seek.setMax(max); seek.setProgress(initial); seek.setContentDescription(title);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(44)); sp.topMargin=dp(6); panel.addView(seek,sp);
        LinearLayout ends=new LinearLayout(this);
        ends.addView(text(left,10,MUTED),new LinearLayout.LayoutParams(0,-2,1));
        ends.addView(text(right,10,MUTED)); panel.addView(ends);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar,int value,boolean fromUser) {
                if(fromUser) { settings.prefs.edit().putInt(key,value).apply(); label.setText(getString(R.string.slider_value,title,value)); preview.refresh(); }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        add(panel,5,7);
    }

    private TextView text(String text,float size,int color) {
        TextView view=new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(color); return view;
    }
    private Button button(String title,boolean primary) {
        Button b=new Button(this); b.setText(title); b.setAllCaps(false); b.setTextColor(primary?BG:INK); b.setTextSize(13);
        b.setBackground(background(primary?ACCENT:CARD,14)); b.setElevation(0); return b;
    }
    private GradientDrawable background(int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private int dp(float value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private void add(View view,int top,int bottom) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(top); p.bottomMargin=dp(bottom); body.addView(view,p);
    }
    @Override protected void onResume() { super.onResume(); if(preview!=null) preview.resume(); if(wallpaperApply!=null) wallpaperApply.onResume(); }
    @Override protected void onPause() { if(wallpaperApply!=null) wallpaperApply.onPause(); if(preview!=null) preview.pause(); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle out) { if(wallpaperApply!=null) wallpaperApply.save(out); super.onSaveInstanceState(out); }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(wallpaperApply!=null) wallpaperApply.onActivityResult(requestCode,resultCode,data);
    }
    @Override protected void onDestroy() { if(wallpaperApply!=null) wallpaperApply.destroy(); if(preview!=null) preview.release(); super.onDestroy(); }
}
