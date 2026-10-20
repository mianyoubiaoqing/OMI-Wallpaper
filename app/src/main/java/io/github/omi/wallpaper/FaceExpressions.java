package io.github.omi.wallpaper;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/** Procedural demo expressions or continuous textured lids for an optional licensed pack. */
final class FaceExpressions {
    private final TextureFaceExpressions texture;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    FaceExpressions(Context context) {
        texture=ArtworkAssets.hasPack(context) ? new TextureFaceExpressions(context) : null;
    }
    void draw(Canvas canvas,float time,float age) { draw(canvas,time,age,age,true); }
    void draw(Canvas canvas,float time,float blinkAge,float tapAge,boolean animated) {
        if(texture!=null) { texture.draw(canvas,time,blinkAge,tapAge,animated); return; }
        drawDemo(canvas,time,blinkAge,tapAge,animated,paint,path);
    }
    static void drawDemo(Canvas canvas,float time,float tapAge,boolean animated) {
        drawDemo(canvas,time,Float.POSITIVE_INFINITY,tapAge,animated,new Paint(Paint.ANTI_ALIAS_FLAG),new Path());
    }
    private static void drawDemo(Canvas c,float time,float blinkAge,float tapAge,boolean animated,Paint p,Path path) {
        eye(c,234,683,animated?BlinkMotion.closure(time,blinkAge,true):0,p,path);
        eye(c,390,700,animated?BlinkMotion.closure(time,blinkAge,false):0,p,path);
        p.setStyle(Paint.Style.FILL); p.setColor(DemoArtwork.SKIN); c.drawRect(258,767,335,837,p);
        p.setColor(DemoArtwork.INK);
        if(animated && tapAge>=0 && tapAge<TapGesture.MOUTH_SECONDS) c.drawOval(283,790,315,825,p);
        else {
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(6); p.setStrokeCap(Paint.Cap.ROUND);
            path.reset(); path.moveTo(274,799); path.quadTo(299,821,324,799); c.drawPath(path,p);
            p.setStyle(Paint.Style.FILL);
        }
    }
    private static void eye(Canvas c,float x,float y,float closure,Paint p,Path path) {
        p.setColor(DemoArtwork.SKIN); c.drawRect(x-43,y-37,x+43,y+37,p);
        p.setColor(0xffffffff); c.drawOval(x-36,y-26,x+36,y+26,p);
        p.setColor(0xff267ba5); c.drawCircle(x,y,19,p);
        p.setColor(DemoArtwork.INK); c.drawCircle(x,y,10,p);
        p.setColor(0xffffffff); c.drawCircle(x-6,y-7,5,p);
        // Move opaque lids over a fixed iris, rather than squash or fade the eye.
        float edge=26*(1-closure);
        p.setColor(DemoArtwork.SKIN);
        c.drawRect(x-40,y-30,x+40,y-edge,p); c.drawRect(x-40,y+edge,x+40,y+30,p);
        p.setColor(DemoArtwork.INK); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(5);
        path.reset(); path.moveTo(x-35,y-edge); path.quadTo(x,y-edge-4*(1-closure),x+35,y-edge);
        c.drawPath(path,p); p.setStyle(Paint.Style.FILL);
    }
    void release() { if(texture!=null) texture.release(); }
}
