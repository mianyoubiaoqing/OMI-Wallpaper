package io.github.omi.wallpaper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Owns launch attempts and their visible outcomes; a successful start is never an apply result. */
final class WallpaperApplyController {
    private static final String PICKER_PACKAGE = "com.android.wallpaper.livepicker";
    private static final int EXPORT_LOG = 8401;
    private final Activity activity;
    private final TextView status;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SharedPreferences history;
    private boolean pending, leftApp, resumed;
    private long started;
    private String route = "";
    private final Runnable noWindow = this::checkNoWindow;
    private void checkNoWindow() {
        if (pending && resumed && !leftApp) {
            pending = false;
            record("no_window_observed: " + route);
            status.setText("系统预览未打开。请点「兼容设置入口」或「其他设置方式」。");
        }
    }

    WallpaperApplyController(Activity activity, TextView status, Bundle saved) {
        this.activity = activity;
        this.status = status;
        history = activity.getSharedPreferences("wallpaper_apply", Activity.MODE_PRIVATE);
        if (saved != null) {
            pending = saved.getBoolean("apply.pending");
            leftApp = saved.getBoolean("apply.left");
            started = saved.getLong("apply.started");
            route = saved.getString("apply.route", "");
            status.setText(saved.getString("apply.status", "在系统页面选择「OMI Wallpaper」并确认"));
        }
    }

    void openDefault() {
        String maker = (Build.MANUFACTURER + " " + Build.BRAND).toLowerCase(Locale.ROOT);
        // On Xiaomi/Redmi prefer the separate installed picker over the OEM default intent handler.
        if (maker.contains("xiaomi") || maker.contains("redmi")) {
            openCompatible();
            return;
        }
        if (!launch(previewIntent(), "系统直接预览")) openCompatible();
    }

    void openCompatible() {
        // Some OEM preview handlers return without displaying a selection window.
        if (!launch(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER), "系统动态壁纸列表")) {
            if (!launch(nativeChooser(), "原生动态壁纸列表"))
                status.setText("未能打开动态壁纸列表。请点「其他设置方式」，或查看诊断信息。");
        }
    }

    private Intent previewIntent() {
        return new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        new ComponentName(activity, OmiWallpaperService.class));
    }

    private Intent nativeChooser() {
        return new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                .setComponent(new ComponentName(PICKER_PACKAGE, PICKER_PACKAGE + ".LiveWallpaperActivity"));
    }

    private Intent nativePreview() {
        return previewIntent().setComponent(new ComponentName(PICKER_PACKAGE, PICKER_PACKAGE + ".LiveWallpaperChange"));
    }

    private boolean usable(Intent intent) {
        try {
            ActivityInfo info = activity.getPackageManager().getActivityInfo(intent.getComponent(), 0);
            return info.exported && info.enabled && info.applicationInfo.enabled;
        } catch (PackageManager.NameNotFoundException | SecurityException e) { return false; }
    }

    private boolean launch(Intent intent, String label) {
        handler.removeCallbacks(noWindow);
        route = label;
        pending = true;
        leftApp = false;
        started = SystemClock.elapsedRealtime();
        status.setText("正在打开" + label + "…请选择「OMI Wallpaper」并确认应用。");
        record("launch: " + label + " / " + intent.getAction() + " / " + intent.getComponent());
        try {
            activity.startActivity(intent);
            handler.postDelayed(noWindow, 1800);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            pending = false;
            record("launch_error: " + e.getClass().getSimpleName() + " / " + label);
            status.setText("此设置入口不可用，请尝试其他设置方式。");
            Toast.makeText(activity, "「" + label + "」不可用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    void showAlternatives() {
        List<String> labels = new ArrayList<>();
        List<Intent> intents = new ArrayList<>();
        labels.add("兼容预览"); intents.add(nativePreview());
        labels.add("原生动态壁纸列表"); intents.add(nativeChooser());
        labels.add("系统动态壁纸列表"); intents.add(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
        // Explicit selection bypasses a broken default handler, while still using installed exported activities.
        for (ResolveInfo result : handlers(previewIntent())) {
            if (!result.activityInfo.exported || !result.activityInfo.enabled) continue;
            ComponentName name = new ComponentName(result.activityInfo.packageName, result.activityInfo.name);
            labels.add("直接预览 · " + result.loadLabel(activity.getPackageManager()) + "\n" + name.getPackageName());
            intents.add(previewIntent().setComponent(name));
        }
        labels.add("系统壁纸设置"); intents.add(new Intent(Intent.ACTION_SET_WALLPAPER));
        labels.add("查看 / 复制诊断信息");
        new AlertDialog.Builder(activity).setTitle("其他设置方式")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    if (which == intents.size()) showDiagnostics();
                    else launch(intents.get(which), labels.get(which).split("\n")[0]);
                }).setNegativeButton("返回", null).show();
    }

    private List<ResolveInfo> handlers(Intent intent) {
        try { return activity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY); }
        catch (SecurityException e) { return new ArrayList<>(); }
    }

    void onResume() {
        resumed = true;
        if (!pending) return;
        if (!leftApp) {
            handler.removeCallbacks(noWindow);
            handler.postDelayed(noWindow, Math.max(100, 1800 - (SystemClock.elapsedRealtime() - started)));
            return;
        }
        pending = false;
        String applied = appliedScreens();
        record("returned: " + route + " / " + applied);
        if (!applied.isEmpty()) status.setText("已检测到「OMI Wallpaper」用于" + applied + "。返回桌面查看效果。");
        else status.setText("已返回应用，尚未检测到壁纸已应用。若预览一闪而过，请点「兼容设置入口」。");
    }

    void onPause() {
        resumed = false;
        if (pending) { leftApp = true; record("activity_paused: " + route); }
        handler.removeCallbacks(noWindow);
    }

    void save(Bundle out) {
        out.putBoolean("apply.pending", pending); out.putBoolean("apply.left", leftApp);
        out.putLong("apply.started", started); out.putString("apply.route", route);
        out.putString("apply.status", status.getText().toString());
    }

    void destroy() { handler.removeCallbacksAndMessages(null); }

    private String appliedScreens() {
        try {
            WallpaperManager manager = WallpaperManager.getInstance(activity);
            boolean home = isOurs(manager.getWallpaperInfo());
            boolean lock = Build.VERSION.SDK_INT >= 34 && isOurs(manager.getWallpaperInfo(WallpaperManager.FLAG_LOCK));
            return home && lock ? "主屏幕与锁屏" : home ? "主屏幕" : lock ? "锁屏" : "";
        } catch (SecurityException e) { return ""; }
    }

    private boolean isOurs(WallpaperInfo info) {
        return info != null && info.getComponent().equals(new ComponentName(activity, OmiWallpaperService.class));
    }

    private void record(String event) {
        AppDiagnostics.event(event);
        String log = history.getString("events", "");
        String[] lines = log.split("\n");
        StringBuilder recent = new StringBuilder();
        for (int i=Math.max(0, lines.length-11); i<lines.length; i++) if (!lines[i].isEmpty()) recent.append(lines[i]).append('\n');
        recent.append(System.currentTimeMillis()).append(' ').append(event).append('\n');
        history.edit().putString("events", recent.toString()).apply();
    }

    void showDiagnostics() {
        String report = diagnosticReport();
        TextView text = new TextView(activity);
        text.setText(report); text.setTextSize(12); text.setTextIsSelectable(true);
        int padding = Math.round(20 * activity.getResources().getDisplayMetrics().density);
        text.setPadding(padding, padding, padding, padding);
        android.widget.ScrollView scroll = new android.widget.ScrollView(activity); scroll.addView(text);
        new AlertDialog.Builder(activity).setTitle("壁纸设置诊断 · "+BuildConfig.VERSION_NAME).setView(scroll)
                .setPositiveButton("导出 TXT 日志", (dialog, which) -> exportLog(report))
                .setNeutralButton("复制日志", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager)activity.getSystemService(Activity.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("OMI 壁纸诊断", report));
                    Toast.makeText(activity, "已复制，可粘贴回对话", Toast.LENGTH_LONG).show();
                }).setNegativeButton("返回", null).show();
    }

    private void exportLog(String report) {
        history.edit().putString("pending_export",report).apply();
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("text/plain").putExtra(Intent.EXTRA_TITLE,"OMI-diagnostics-"+System.currentTimeMillis()+".txt");
        try { activity.startActivityForResult(intent,EXPORT_LOG); }
        catch(ActivityNotFoundException | SecurityException e) {
            record("export_picker_error="+e.getClass().getSimpleName());
            Toast.makeText(activity,"文件保存页面不可用，请使用「复制日志」",Toast.LENGTH_LONG).show();
        }
    }

    void onActivityResult(int requestCode,int resultCode,Intent data) {
        if(requestCode!=EXPORT_LOG || resultCode!=Activity.RESULT_OK || data==null || data.getData()==null) return;
        try(OutputStream output=activity.getContentResolver().openOutputStream(data.getData(),"wt")) {
            if(output==null) throw new IOException("No output stream");
            output.write(history.getString("pending_export",diagnosticReport()).getBytes(StandardCharsets.UTF_8));
            history.edit().remove("pending_export").apply();
            record("log_exported");
            Toast.makeText(activity,"日志已保存，请将 TXT 文件发回对话",Toast.LENGTH_LONG).show();
        } catch(IOException | SecurityException e) {
            record("export_write_error="+e.getClass().getSimpleName());
            Toast.makeText(activity,"保存失败，请用「复制日志」",Toast.LENGTH_LONG).show();
        }
    }

    private String diagnosticReport() {
        StringBuilder report = new StringBuilder("OMI Wallpaper ").append(BuildConfig.VERSION_NAME).append(" / 设置诊断\n");
        report.append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
        report.append("Android ").append(Build.VERSION.RELEASE).append(" / API ").append(Build.VERSION.SDK_INT).append('\n');
        report.append("系统版本：").append(Build.DISPLAY).append(" / ").append(Build.VERSION.INCREMENTAL).append('\n');
        report.append("当前状态：").append(status.getText()).append('\n');
        report.append("已应用屏幕：").append(appliedScreens()).append('\n');
        try {
            WallpaperManager manager = WallpaperManager.getInstance(activity);
            report.append("wallpaperSupported=").append(manager.isWallpaperSupported())
                    .append("; setAllowed=").append(manager.isSetWallpaperAllowed()).append('\n');
        } catch (SecurityException e) { report.append("policy_check=SecurityException\n"); }
        report.append("兼容预览可用：").append(usable(nativePreview())).append('\n');
        report.append("兼容列表可用：").append(usable(nativeChooser())).append('\n');
        for (Intent intent : new Intent[]{previewIntent(), new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER), new Intent(Intent.ACTION_SET_WALLPAPER)}) {
            report.append('\n').append(intent.getAction()).append('\n');
            List<ResolveInfo> found = handlers(intent);
            if (found.isEmpty()) report.append("没有可见的处理程序\n");
            for (ResolveInfo info : found) report.append(info.activityInfo.packageName).append('/').append(info.activityInfo.name)
                    .append(" exported=").append(info.activityInfo.exported).append(" enabled=").append(info.activityInfo.enabled).append('\n');
        }
        SceneSettings scene=new SceneSettings(activity);
        report.append("\n动效配置：motion=").append(scene.motion).append(" touch=").append(scene.touch)
                .append(" tilt=").append(scene.tilt).append(" particles=").append(scene.particles)
                .append(" expressions=").append(scene.expressions).append(" software=").append(scene.software)
                .append(" character=").append(scene.character).append(" poseStrength=").append(scene.poseStrength)
                .append(" handStrength=").append(scene.handStrength)
                .append(" fps=").append(scene.fps).append(" strength=").append(scene.strength).append('\n');
        report.append("\n").append(AppDiagnostics.report());
        report.append("\n日志仅含本应用运行信息；不会自动上传。\n复现建议：轻触预览，再点设置壁纸，等待 3 秒后导出。\n");
        return report.toString();
    }
}
