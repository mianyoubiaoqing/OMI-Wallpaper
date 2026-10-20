package io.github.omi.wallpaper;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small local event journal: no network upload, device identifiers, or other apps' logs. */
public final class AppDiagnostics extends Application {
    private static SharedPreferences journal;
    private static long sessionStart;
    private static long previewFrames, previewTouches;
    private static boolean previewRunning;
    private static int nextEngine;
    private static final Map<Integer,EngineStats> engines=new LinkedHashMap<>();

    @Override public void onCreate() {
        super.onCreate();
        journal=getSharedPreferences("diagnostics", MODE_PRIVATE);
        sessionStart=SystemClock.elapsedRealtime();
        event("app_start version="+BuildConfig.VERSION_NAME);
        Thread.UncaughtExceptionHandler original=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error)-> {
            String trace=Log.getStackTraceString(error);
            journal.edit().putString("last_crash", trace.substring(0,Math.min(trace.length(),12000))).commit();
            if(original!=null) original.uncaughtException(thread,error);
            else { android.os.Process.killProcess(android.os.Process.myPid()); System.exit(10); }
        });
    }

    static synchronized void event(String message) {
        if(journal==null) return;
        String previous=journal.getString("events", "");
        String[] lines=previous.split("\n");
        StringBuilder result=new StringBuilder();
        for(int i=Math.max(0,lines.length-59);i<lines.length;i++) if(!lines[i].isEmpty()) result.append(lines[i]).append('\n');
        result.append(new SimpleDateFormat("MM-dd HH:mm:ss.SSS",Locale.ROOT).format(new Date()))
                .append(' ').append(message.replace('\n',' ')).append('\n');
        journal.edit().putString("events",result.toString()).apply();
    }

    static void previewFrame() { previewFrames++; }
    static void previewTouch() { previewTouches++; }
    static void previewState(boolean running) { previewRunning=running; event("preview_running="+running); }

    static int engineCreated(boolean preview) {
        int id=++nextEngine;
        if(engines.size()>=8) engines.remove(engines.keySet().iterator().next());
        engines.put(id,new EngineStats(preview ? "系统预览" : "已应用壁纸"));
        event("engine#"+id+" created type="+engines.get(id).type);
        return id;
    }
    static void engineVisible(int id,boolean visible) {
        EngineStats stats=engines.get(id); if(stats==null) return;
        long now=SystemClock.elapsedRealtime();
        if(stats.visible) stats.visibleMillis+=now-stats.visibleStart;
        stats.visible=visible; stats.visibleStart=now;
        event("engine#"+id+" visible="+visible+" posted="+stats.frames+" raw="+stats.touches+" tapCommands="+stats.commands);
        if(!visible) journal.edit().putString("last_engine_session",engineReport()).apply();
    }
    static void enginePosted(int id,boolean hardware,float tiltX,float tiltY) {
        EngineStats stats=engines.get(id); if(stats==null) return;
        stats.frames++; stats.lastFrame=SystemClock.elapsedRealtime();
        stats.hardware=hardware; stats.tiltX=tiltX; stats.tiltY=tiltY;
    }
    static void engineTouch(int id,int action,float x,float y) {
        EngineStats stats=engines.get(id); if(stats==null) return;
        stats.touches++;
        if(action==0) event("engine#"+id+" raw_DOWN x="+Math.round(x)+" y="+Math.round(y));
    }
    static void engineCommand(int id) {
        EngineStats stats=engines.get(id); if(stats==null) return;
        stats.commands++; event("engine#"+id+" COMMAND_TAP");
    }
    static void engineOffset(int id) {
        EngineStats stats=engines.get(id); if(stats!=null) stats.offsets++;
    }
    static void engineDestroyed(int id) {
        engineVisible(id,false); event("engine#"+id+" destroyed");
    }

    private static String engineReport() {
        StringBuilder text=new StringBuilder(); long now=SystemClock.elapsedRealtime();
        for(Map.Entry<Integer,EngineStats> entry:engines.entrySet()) {
            EngineStats s=entry.getValue();
            long millis=s.visibleMillis+(s.visible ? now-s.visibleStart : 0);
            text.append("engine#").append(entry.getKey()).append(' ').append(s.type)
                    .append(" visible=").append(s.visible).append(" postedFrames=").append(s.frames)
                    .append(" visibleMs=").append(millis)
                    .append(" rawTouchEvents=").append(s.touches).append(" tapCommands=").append(s.commands)
                    .append(" offsetCallbacks=").append(s.offsets).append(" canvas=").append(s.hardware ? "hardware" : "software")
                    .append(" tilt=").append(String.format(Locale.ROOT,"%.2f,%.2f",s.tiltX,s.tiltY))
                    .append(" msSinceLastPost=").append(s.lastFrame==0 ? -1 : now-s.lastFrame).append('\n');
        }
        return text.toString();
    }

    private static final class EngineStats {
        final String type;
        long frames,touches,commands,offsets,visibleMillis,visibleStart,lastFrame;
        boolean visible,hardware;
        float tiltX,tiltY;
        EngineStats(String type) { this.type=type; }
    }

    static String report() {
        return "本次进程运行秒数="+(SystemClock.elapsedRealtime()-sessionStart)/1000
                +"\n预览 running="+previewRunning+" frames="+previewFrames+" touchEvents="+previewTouches
                +"\n\n按引擎区分（postedFrames 为成功提交的画布）\n"+engineReport()
                +(engines.isEmpty() ? "最近保存的引擎记录：\n"+journal.getString("last_engine_session","无\n") : "")
                +"\n\n最近运行日志（最多 60 条）\n"+journal.getString("events", "")
                +"\n最近一次未捕获异常\n"+journal.getString("last_crash", "未记录\n");
    }
}
