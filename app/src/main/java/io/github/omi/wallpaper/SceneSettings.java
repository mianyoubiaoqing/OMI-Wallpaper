package io.github.omi.wallpaper;

import android.content.Context;
import android.content.SharedPreferences;

final class SceneSettings {
    final SharedPreferences prefs;
    boolean motion, character, touch, tilt, particles, expressions, software;
    float strength, poseStrength, handStrength, framing, zoom;
    int fps;

    SceneSettings(Context context) {
        prefs = context.getSharedPreferences("scene", Context.MODE_PRIVATE);
        reload();
    }

    void reload() {
        motion = prefs.getBoolean("motion",true);
        character = prefs.getBoolean("character",true);
        poseStrength = SceneMath.clamp(prefs.getInt("poseStrength",80)/100f,0,1);
        handStrength = SceneMath.clamp(prefs.getInt("handStrength",100)/100f,0,1);
        touch = prefs.getBoolean("touch",true);
        tilt = prefs.getBoolean("tilt",true);
        particles = prefs.getBoolean("particles",true);
        expressions = prefs.getBoolean("expressions",true);
        software = prefs.getBoolean("software",false);
        strength = SceneMath.clamp(prefs.getInt("strength",45)/100f,0,1);
        framing = SceneMath.clamp(prefs.getInt("framing",18)/100f,0,1);
        zoom = 1+SceneMath.clamp(prefs.getInt("zoom",0)/100f,0,.30f);
        fps = prefs.getBoolean("smooth",false) ? 60 : 30;
    }
}
