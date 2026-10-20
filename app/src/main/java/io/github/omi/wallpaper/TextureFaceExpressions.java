package io.github.omi.wallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import java.io.IOException;
import java.io.InputStream;

/** Small feathered expression patches, aligned to the untouched 1240 x 1754 original. */
final class TextureFaceExpressions {
    private final Patch[] left=new Patch[2],right=new Patch[2];
    private final Patch mouth;
    private final EyeLid leftLid,rightLid;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    TextureFaceExpressions(Context context) {
        loadEyes(context,"original.png",0,new float[]{1,0,0,0,1,0,0,0,1});
        mouth=loadEyes(context,"expression-closed.png",1,new float[]{1,0,0,0,1,0,0,0,1});
        // Lower edges of the upper lashes and the lower eye rim, in original coordinates.
        // Opaque upper/lower lids cover an UNWARPED iris; no texture fading or iris squashing.
        leftLid=new EyeLid(left,new float[]{192,208,224,240,256,276},new float[][]{
                {652,657,655,662,677,690},{668,682,707,706,704,695},
                {652,674,681,687,692,695}});
        rightLid=new EyeLid(right,new float[]{323,344,368,392,416,440,461},new float[][]{
                {677,671,668,676,685,695,698},{694,704,713,726,724,712,706},
                {694,703,710,716,722,715,706}});
    }

    private Patch loadEyes(Context context,String name,int index,float[] matrix) {
        try {
            Bitmap source=ArtworkAssets.load(context,name);
            if(source==null) throw new IOException("Expression texture could not be decoded");
            Matrix alignment=new Matrix();
            alignment.setValues(matrix);
            left[index]=new Patch(source,alignment,new Rect(192,632,276,723));
            right[index]=new Patch(source,alignment,new Rect(323,650,461,740));
            Patch smile=index==1 ? new Patch(source,alignment,new Rect(260,770,327,841)) : null;
            source.recycle();
            return smile;
        } catch(IOException error) { throw new IllegalStateException("Expression artwork missing",error); }
    }

    void draw(Canvas canvas,float time,float responseAge) {
        draw(canvas,time,responseAge,responseAge,true);
    }

    void draw(Canvas canvas,float time,float blinkAge,float tapAge,boolean animated) {
        if(animated) {
            leftLid.draw(canvas,BlinkMotion.closure(time,blinkAge,true));
            rightLid.draw(canvas,BlinkMotion.closure(time,blinkAge,false));
        }
        // The original mouth is open. Cover it with an opaque smile at ALL idle times,
        // including when expression animation is disabled. Only a confirmed tap reveals it.
        if(!animated || tapAge<0 || tapAge>=TapGesture.MOUTH_SECONDS) drawPatch(canvas,mouth);
    }

    private void drawPatch(Canvas canvas,Patch patch) {
        // Only patch edges are feathered. Eyelids/irises are always opaque, so the open
        // eye cannot show through the closed eye during a blink.
        paint.setAlpha(255);
        canvas.drawBitmap(patch.bitmap,patch.rect.left,patch.rect.top,paint);
    }

    void release() {
        for(Patch patch:left) patch.bitmap.recycle();
        for(Patch patch:right) patch.bitmap.recycle();
        mouth.bitmap.recycle();
        leftLid.release(); rightLid.release();
    }

    /** Two opaque moving lids. The original iris remains stationary in the exposed opening. */
    private static final class EyeLid {
        private static final int COLS=28,ROWS=36;
        private final Patch[] poses;
        private final float[] xs;
        private final float[][] lids;
        private final float[] vertices=new float[(COLS+1)*(ROWS+1)*2];
        private final Paint outputPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        private final Path clip=new Path();
        private final Bitmap lidTexture,foreground;

        EyeLid(Patch[] poses,float[] xs,float[][] lids) {
            this.poses=poses; this.xs=xs; this.lids=lids;
            lidTexture=poses[1].bitmap.copy(Bitmap.Config.ARGB_8888,true);
            foreground=Bitmap.createBitmap(lidTexture.getWidth(),lidTexture.getHeight(),Bitmap.Config.ARGB_8888);
            separateForeground(poses[0].bitmap,poses[1].bitmap);
        }

        void draw(Canvas target,float closure) {
            if(closure<=0) return; // Exact original artwork at rest.
            Rect rect=poses[0].rect;
            target.save(); target.translate(rect.left,rect.top);
            drawLid(target,closure,true);
            drawLid(target,closure,false);
            target.drawBitmap(foreground,0,0,outputPaint);
            target.restore();
        }

        private void drawLid(Canvas target,float closure,boolean upper) {
            Rect rect=poses[1].rect;
            float w=rect.width(),h=rect.height();
            clip.reset();
            for(int col=0;col<=COLS;col++) {
                float x=col*w/COLS;
                float closedEdge=curve(2,x+rect.left)-rect.top;
                float oldLid=closedEdge+(upper?0:9);
                float edge=curve(upper?0:1,x+rect.left)-rect.top;
                float nextLid=edge*(1-closure)+(closedEdge-(upper?0:1))*closure;
                if(col==0) clip.moveTo(x,nextLid); else clip.lineTo(x,nextLid);
                for(int row=0;row<=ROWS;row++) {
                    float y=row*h/ROWS;
                    float mapped=y<=oldLid ? y*nextLid/oldLid : nextLid+(y-oldLid)*(h-nextLid)/(h-oldLid);
                    int index=(row*(COLS+1)+col)*2;
                    vertices[index]=x; vertices[index+1]=mapped;
                }
            }
            // Only cover the eye's motion envelope, keeping the brow and cheek still.
            for(int col=COLS;col>=0;col--) {
                float x=col*w/COLS;
                float bound=upper ? curve(0,x+rect.left)-rect.top-12 : h;
                clip.lineTo(x,bound);
            }
            clip.close();
            target.save(); target.clipPath(clip);
            target.drawBitmapMesh(lidTexture,COLS,ROWS,vertices,0,null,0,outputPaint);
            target.restore();
        }

        private void separateForeground(Bitmap original,Bitmap closed) {
            int w=closed.getWidth(),h=closed.getHeight();
            boolean[] originalHair=hairMask(original,true),closedHair=hairMask(closed,false);
            for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
                if(originalHair[y*w+x]) foreground.setPixel(x,y,original.getPixel(x,y));
                if(!closedHair[y*w+x]) continue;
                int replacement=closed.getPixel(x,y);
                // In the moving lid's texture only, extend adjacent skin under a fixed
                // foreground strand. Hair is then drawn once at its original coordinates.
                search: for(int radius=1;radius<60;radius++) {
                    for(int dx=-radius;dx<=radius;dx++) for(int sign=-1;sign<=1;sign+=2) {
                        int sx=x+dx,sy=y+sign*(radius-Math.abs(dx));
                        if(sx<0 || sx>=w || sy<0 || sy>=h || closedHair[sy*w+sx]) continue;
                        int color=closed.getPixel(sx,sy),r=(color>>16)&255,g=(color>>8)&255,b=color&255;
                        if(r>190 && g>140 && r>g+15 && r>b+25) { replacement=color; break search; }
                    }
                }
                lidTexture.setPixel(x,y,(closed.getPixel(x,y)&0xff000000)|(replacement&0xffffff));
            }
        }

        private boolean[] hairMask(Bitmap bitmap,boolean original) {
            int w=bitmap.getWidth(),h=bitmap.getHeight();
            boolean[] mask=new boolean[w*h];
            for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
                float px=x+poses[0].rect.left,py=y+poses[0].rect.top;
                float strandDistance=Math.abs(px-(425-(py-660)*.125f));
                if(original && poses[0].rect.left>300 && strandDistance<4.4f) {
                    mask[y*w+x]=true; continue;
                }
                boolean nearHair=original ? (poses[0].rect.left<300 ? px<207 : px>449) :
                        py<curve(2,px)-11 || (poses[0].rect.left<300 ? px<210 : strandDistance<6 || px>449);
                if(!nearHair) continue;
                int c=bitmap.getPixel(x,y),r=(c>>16)&255,g=(c>>8)&255,b=c&255;
                if(g>r+5 && g>b+5) for(int dy=-2;dy<=2;dy++) for(int dx=-2;dx<=2;dx++) {
                    int xx=x+dx,yy=y+dy;
                    if(xx>=0 && xx<w && yy>=0 && yy<h) mask[yy*w+xx]=true;
                }
            }
            return mask;
        }

        private float curve(int pose,float x) {
            for(int i=1;i<xs.length;i++) if(x<=xs[i]) {
                float t=(x-xs[i-1])/(xs[i]-xs[i-1]);
                return lids[pose][i-1]*(1-t)+lids[pose][i]*t;
            }
            return lids[pose][xs.length-1];
        }
        void release() { lidTexture.recycle(); foreground.recycle(); }
    }

    private static final class Patch {
        final Rect rect;
        final Bitmap bitmap;
        Patch(Bitmap source,Matrix alignment,Rect rect) {
            this.rect=rect;
            bitmap=Bitmap.createBitmap(rect.width(),rect.height(),Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(bitmap);
            Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.save(); canvas.translate(-rect.left,-rect.top);
            canvas.drawBitmap(source,alignment,paint); canvas.restore();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            int[] colors={0x00ffffff,0xffffffff,0xffffffff,0x00ffffff};
            float[] stops={0,.08f,.92f,1};
            paint.setShader(new LinearGradient(0,0,rect.width(),0,colors,stops,Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,rect.width(),rect.height(),paint);
            paint.setShader(new LinearGradient(0,0,0,rect.height(),colors,stops,Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,rect.width(),rect.height(),paint);
        }
    }
}
