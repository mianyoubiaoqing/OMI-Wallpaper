package io.github.omi.wallpaper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/** Original geometric robot, drawn entirely in code and licensed with the engine. */
final class DemoArtwork {
    static final int SKIN=0xffffdfb5,INK=0xff173b45;
    private DemoArtwork() {}
    private static Bitmap bitmap() { return Bitmap.createBitmap(1240,1754,Bitmap.Config.ARGB_8888); }
    private static Paint paint(int color) {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(color); p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND); return p;
    }
    static Bitmap body() {
        Bitmap b=bitmap(); Canvas c=new Canvas(b); c.drawColor(0xffeff5ed);
        Paint p=paint(0xffdae9d9);
        for(int i=0;i<6;i++) c.drawCircle(1100-i*80,240+i*255,120+i*10,p);
        p.setColor(0xffb0d8ca); c.drawRoundRect(360,850,795,1535,160,160,p);
        p.setColor(INK); c.drawRoundRect(415,925,710,1400,90,90,p);
        p.setColor(0xffffcb75); c.drawCircle(555,1110,60,p);
        p.setColor(0xff74b7ad); c.drawRoundRect(400,1450,515,1754,45,45,p);
        c.drawRoundRect(625,1450,740,1754,45,45,p);
        p.setColor(0xffffcb75); c.drawRoundRect(440,1325,680,1355,15,15,p);
        return b;
    }
    static Bitmap head() {
        Bitmap b=bitmap(); Canvas c=new Canvas(b); Paint p=paint(0xff1a6d70);
        c.drawRoundRect(115,425,580,905,145,145,p);
        p.setColor(SKIN); c.drawRoundRect(170,585,490,865,95,95,p);
        p.setColor(0xff8cd9b0); c.drawRoundRect(160,445,535,600,70,70,p);
        p.setColor(INK); p.setStrokeWidth(18); c.drawLine(350,450,350,345,p);
        p.setColor(0xffffcb75); c.drawCircle(350,320,40,p);
        Path lock=new Path(); lock.moveTo(105,735); lock.cubicTo(85,850,95,1020,180,1110);
        lock.cubicTo(200,990,160,850,160,735); lock.close();
        p.setColor(0xff3eaa91); c.drawPath(lock,p);
        lock.reset(); lock.moveTo(495,730); lock.cubicTo(550,870,520,1010,425,1125);
        lock.cubicTo(575,1080,610,890,555,730); lock.close(); c.drawPath(lock,p);
        lock.reset(); lock.moveTo(650,835); lock.cubicTo(800,850,960,1000,1240,950);
        lock.lineTo(1240,1005); lock.cubicTo(970,1080,800,930,650,880); lock.close();
        p.setColor(0xff83cbb5); c.drawPath(lock,p);
        FaceExpressions.drawDemo(c,0,Float.POSITIVE_INFINITY,false);
        return b;
    }
    static Bitmap hand() {
        Bitmap b=bitmap(); Canvas c=new Canvas(b); Paint p=paint(0xff457fbb);
        p.setStrokeWidth(80); c.drawLine(385,1390,205,1135,p); c.drawLine(205,1135,180,1000,p);
        p.setStrokeWidth(35); c.drawLine(180,1010,120,875,p);
        c.drawLine(185,1015,245,875,p); c.drawLine(200,1035,290,975,p);
        p.setColor(0xffa3cde5); c.drawCircle(187,1050,40,p);
        return b;
    }
    static Bitmap original() {
        Bitmap b=body(),h=head(),g=hand(); Canvas c=new Canvas(b); Paint p=paint(0xffffffff);
        c.drawBitmap(h,0,0,p); c.drawBitmap(g,0,0,p); h.recycle(); g.recycle(); return b;
    }
}
