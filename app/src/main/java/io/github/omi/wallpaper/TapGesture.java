package io.github.omi.wallpaper;

/** Separates a short, stationary click from drag feedback and duplicate launcher commands. */
final class TapGesture {
    static final float MOUTH_SECONDS=1.8f;
    private final float slopSquared;
    private float downX,downY;
    private long downAt,lastRawEnd=Long.MIN_VALUE/2,lastTap=Long.MIN_VALUE/2;
    private boolean down,moved;

    TapGesture(float slop) { slopSquared=slop*slop; }
    void down(float x,float y,long now) { down=true; moved=false; downX=x; downY=y; downAt=now; }
    void move(float x,float y) {
        if(down && (x-downX)*(x-downX)+(y-downY)*(y-downY)>slopSquared) moved=true;
    }
    boolean up(float x,float y,long now) {
        move(x,y);
        boolean tapped=down && !moved && now-downAt<=450;
        down=false; lastRawEnd=now;
        if(tapped && now-lastTap>160) { lastTap=now; return true; }
        return false;
    }
    void cancel(long now) { down=false; moved=false; lastRawEnd=now; }
    boolean command(long now) {
        // Some launchers send COMMAND_TAP before UP. Let the raw gesture decide in that case.
        if(down || now-lastRawEnd<500 || now-lastTap<500) return false;
        lastTap=now; return true;
    }
    void reset(long now) { cancel(now); }
}
