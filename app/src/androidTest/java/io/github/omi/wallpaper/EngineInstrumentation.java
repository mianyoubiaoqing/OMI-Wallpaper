package io.github.omi.wallpaper;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import java.io.File;
import java.io.FileOutputStream;

/** No third-party assets: exercise the real renderer and shared demo layers. */
public final class EngineInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            Context context=getTargetContext();
            if(ArtworkAssets.hasPack(context)) throw new AssertionError("Run demo checks without a custom texture pack");
            Bitmap original=ArtworkAssets.original(context);
            LayeredArtwork a=LayeredArtwork.acquire(context,original),b=LayeredArtwork.acquire(context,original);
            a.release();
            if(b.head.isRecycled()) throw new AssertionError("Shared texture recycled too early");
            b.release(); original.recycle();

            SceneSettings settings=new SceneSettings(context);
            settings.character=true; settings.motion=true; settings.poseStrength=1; settings.handStrength=1;
            settings.expressions=true; settings.touch=false; settings.tilt=false; settings.particles=false;
            long[] now={10000};
            SceneRenderer renderer=new SceneRenderer(context,settings,()->now[0]);
            Bitmap first=Bitmap.createBitmap(620,877,Bitmap.Config.ARGB_8888);
            Bitmap next=Bitmap.createBitmap(620,877,Bitmap.Config.ARGB_8888);
            try {
                renderer.draw(new Canvas(first),620,877);
                for(int i=0;i<25;i++) { now[0]+=100; renderer.draw(new Canvas(next),620,877); }
                int moving=changed(first,next);
                if(moving<200) throw new AssertionError("No visible demo motion: "+moving);
                save(context,first,"demo-start.png"); save(context,next,"demo-motion.png");
                settings.motion=false; settings.character=false; settings.expressions=false;
                renderer.draw(new Canvas(first),620,877); now[0]+=100;
                renderer.draw(new Canvas(next),620,877);
                if(changed(first,next)!=0) throw new AssertionError("Disabled scene is not static");
                FaceExpressions face=new FaceExpressions(context);
                try {
                    new Canvas(first).drawColor(DemoArtwork.SKIN); new Canvas(next).drawColor(DemoArtwork.SKIN);
                    Canvas c1=new Canvas(first),c2=new Canvas(next); c1.scale(.5f,.5f); c2.scale(.5f,.5f);
                    face.draw(c1,0,20,20,true); face.draw(c2,18.3f,20,20,true);
                    if(changed(first,next)<30) throw new AssertionError("Blink does not close visibly");
                    new Canvas(next).drawColor(DemoArtwork.SKIN);
                    face.draw(c2,0,20,.2f,true);
                    if(changed(first,next)<30) throw new AssertionError("Tap mouth does not open");
                } finally { face.release(); }
                result.putString("stream","PASS: procedural_demo; shared_layers; animated_pixels="+moving+"; static_mode; continuous_blink; tap_mouth\n");
            } finally { renderer.release(); first.recycle(); next.recycle(); }
            finish(Activity.RESULT_OK,result);
        } catch(Throwable e) {
            result.putString("stream","FAIL: "+e+"\n"); finish(Activity.RESULT_CANCELED,result);
        }
    }
    private static int changed(Bitmap a,Bitmap b) {
        int total=0;
        for(int y=0;y<a.getHeight();y+=2) for(int x=0;x<a.getWidth();x+=2) {
            int c=a.getPixel(x,y),d=b.getPixel(x,y);
            if(Math.abs(((c>>16)&255)-((d>>16)&255))+Math.abs(((c>>8)&255)-((d>>8)&255))+Math.abs((c&255)-(d&255))>24) total++;
        }
        return total;
    }
    private static void save(Context context,Bitmap bitmap,String name) throws Exception {
        try(FileOutputStream out=new FileOutputStream(new File(context.getFilesDir(),name))) {
            bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
        }
    }
}
