package io.github.omi.wallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.function.LongSupplier;

/** One renderer for both app preview and the Android wallpaper surface. */
final class SceneRenderer {
    private static final int COLS=84, ROWS=120;
    private final Bitmap art;
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint fx = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] vertices = new float[(COLS+1)*(ROWS+1)*2];
    private final float[] hair = new float[(COLS+1)*(ROWS+1)];
    private final float[] layeredHair = new float[hair.length];
    private final float[] pin = new float[hair.length];
    private final float[] posePin = new float[hair.length];
    private final float[] headWeights = new float[hair.length];
    private final float[] handWeights = new float[hair.length];
    private final float[] armWeights = new float[hair.length];
    private final float[] torsoWeights = new float[hair.length];
    private final CharacterMotion character=new CharacterMotion();
    private final HairMotion hairMotion=new HairMotion();
    private LayeredArtwork layers;
    private final Path bodyOcclusion=new Path();
    private final Path frontHead=new Path();
    private final ArrayList<Burst> bursts = new ArrayList<>();
    private final Random random = new Random(31415);
    private final float[] seeds = new float[72];
    private float dragX,dragY,followX,followY,tiltX,tiltY;
    private long lastFrame, lastBurst;
    private float time;
    private int width=1,height=1;
    private boolean down;
    private final SceneSettings settings;
    private final Context contextForLayers;
    private final LongSupplier clock;
    private final FaceExpressions expressions;
    private final TapGesture tapGesture;
    private long tappedAt=-1;
    private float reactionAt=-20, launcherOffset;
    private final Path petal=new Path();

    SceneRenderer(Context context,SceneSettings settings) {
        this(context,settings,SystemClock::uptimeMillis);
    }

    SceneRenderer(Context context,SceneSettings settings,LongSupplier clock) {
        this.settings=settings;
        contextForLayers=context.getApplicationContext();
        this.clock=clock;
        tapGesture=new TapGesture(ViewConfiguration.get(context).getScaledTouchSlop());
        expressions=new FaceExpressions(context);
        art=ArtworkAssets.original(context);
        for (int row=0;row<=ROWS;row++) for (int col=0;col<=COLS;col++) {
            int i=row*(COLS+1)+col;
            float x=(float)col/COLS, y=(float)row/ROWS;
            hair[i]=SceneMath.hairWeight(x,y);
            layeredHair[i]=SceneMath.layeredHairWeight(x,y);
            pin[i]=SceneMath.edgePin(x,y);
            posePin[i]=SceneMath.smooth(0,.012f,x)*(1-SceneMath.smooth(.988f,1,x))*
                    SceneMath.smooth(0,.01f,y)*(1-SceneMath.smooth(.99f,1,y));
            headWeights[i]=CharacterMotion.headWeight(x*art.getWidth(),y*art.getHeight());
            handWeights[i]=CharacterMotion.handWeight(x*art.getWidth(),y*art.getHeight());
            armWeights[i]=CharacterMotion.armWeight(x*art.getWidth(),y*art.getHeight());
            torsoWeights[i]=CharacterMotion.torsoWeight(x*art.getWidth(),y*art.getHeight());
        }
        for(int i=0;i<seeds.length;i++) seeds[i]=random.nextFloat();
        ShoulderSurface.trace(bodyOcclusion);
        frontHead.addRect(0,0,600,1754,Path.Direction.CW);
        frontHead.addRect(0,0,1240,812,Path.Direction.CW);
    }

    void setTilt(float x,float y) { tiltX=x; tiltY=y; }
    void setLauncherOffset(float offset) { launcherOffset=SceneMath.clamp((offset-.5f)*2,-1,1); }

    void touch(MotionEvent event) {
        if(!settings.touch) return;
        float x=event.getX(), y=event.getY();
        long now=clock.getAsLong();
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                tapGesture.down(x,y,now);
                down=true; burst(x,y); updateDrag(x,y); break;
            case MotionEvent.ACTION_MOVE:
                tapGesture.move(x,y);
                updateDrag(x,y);
                if (clock.getAsLong()-lastBurst>200) burst(x,y);
                break;
            case MotionEvent.ACTION_UP:
                if(tapGesture.up(x,y,now)) tappedAt=now;
                down=false; dragX=dragY=0; break;
            case MotionEvent.ACTION_CANCEL:
                tapGesture.cancel(now);
                down=false; dragX=dragY=0; break;
            case MotionEvent.ACTION_POINTER_DOWN:
                tapGesture.cancel(now); break;
            default: break;
        }
    }

    private void updateDrag(float x,float y) {
        dragX=SceneMath.clamp((x/width-.5f)*2,-1,1);
        dragY=SceneMath.clamp((y/height-.5f)*2,-1,1);
    }

    void commandTap(float x,float y) {
        long now=clock.getAsLong();
        if(settings.touch && tapGesture.command(now)) { tappedAt=now; burst(x,y); }
    }

    private void burst(float x,float y) {
        if(bursts.size()>=10) bursts.remove(0);
        bursts.add(new Burst(x/width,y/height,time));
        lastBurst=clock.getAsLong();
        // A drag can emit particles repeatedly without restarting the character's blink.
        if(time-reactionAt>2.1f) reactionAt=time;
    }

    void pause() {
        lastFrame=0;
        down=false; dragX=dragY=followX=followY=0;
        bursts.clear();
        reactionAt=-20;
        tappedAt=-1; tapGesture.reset(clock.getAsLong());
    }

    boolean animated() {
        return settings.motion || (settings.character && settings.poseStrength>0) || settings.expressions || settings.particles || settings.tilt || down ||
                !bursts.isEmpty() || Math.abs(followX)+Math.abs(followY)>.001f;
    }

    void draw(Canvas canvas, int w,int h) {
        if (w<1 || h<1 || art.isRecycled()) return;
        width=w; height=h;
        long now=clock.getAsLong();
        // Keep the ten-second pose cycle in real time even in the 15 fps power-saving mode.
        float dt=lastFrame==0 ? 0 : Math.min(.1f,(now-lastFrame)/1000f);
        lastFrame=now;
        time+=dt;
        if(!settings.touch) { bursts.clear(); down=false; dragX=dragY=0; tappedAt=-1; tapGesture.reset(now); }
        float smoothing=1-(float)Math.exp(-dt*7);
        followX+=(dragX-followX)*smoothing;
        followY+=(dragY-followY)*smoothing;
        float age=time-reactionAt;
        float gust=settings.touch && age<2.1f ? (float)Math.sin(Math.min(1,age/2.1f)*Math.PI) : 0;
        float mx=(settings.tilt ? -tiltX : 0)*.8f+followX*1.8f+(settings.touch ? launcherOffset*.35f : 0);
        float my=(settings.tilt ? tiltY : 0)*.7f+followY*.8f;
        float scale=SceneMath.coverScale(w,h,art.getWidth(),art.getHeight(),settings.zoom);
        float sw=art.getWidth()*scale,sh=art.getHeight()*scale;
        float ox=SceneMath.offset(w,sw,settings.framing,mx*w*.035f);
        float oy=SceneMath.offset(h,sh,.25f,my*h*.018f);
        canvas.drawColor(Color.rgb(246,248,244));
        canvas.save();
        canvas.translate(ox,oy); canvas.scale(scale,scale);
        float poseAmount=settings.character ? settings.poseStrength : 0;
        character.update(time,poseAmount,settings.handStrength,followX,followY,settings.touch ? age : 20);
        if(poseAmount>0) {
            if(layers==null) layers=LayeredArtwork.acquire(contextForLayers,art);
            drawLayered(canvas,poseAmount,age,gust);
        } else if(settings.motion && settings.strength>0) {
            float amplitude=settings.motion ? art.getWidth()*.016f*settings.strength*(1+gust*.25f) : 0;
            for(int row=0;row<=ROWS;row++) for(int col=0;col<=COLS;col++) {
                int index=row*(COLS+1)+col;
                float x=(float)col/COLS, y=(float)row/ROWS;
                // A delayed wave travels from fixed roots toward the tips. Skin and costume
                // have zero weights, including a margin wider than a mesh triangle.
                float wave=(float)Math.sin(time*1.35f-x*2.4f-y*.6f);
                float secondary=(float)Math.sin(time*2.05f-x*3.5f+y*.8f);
                float movement=amplitude*hair[index]*pin[index];
                character.deform(x*art.getWidth(),y*art.getHeight(),headWeights[index],handWeights[index],
                        armWeights[index],torsoWeights[index],posePin[index],vertices,index*2);
                vertices[index*2]+=(wave*.35f+secondary*.12f+followX*.2f)*movement;
                vertices[index*2+1]+=(wave+secondary*.16f+followY*.15f)*movement;
                // A small follow-through on the free hair ends lags the head pose.
                vertices[index*2+1]+=(float)Math.sin((time-.24f)*.6283185f)*poseAmount*5*hair[index]*pin[index];
            }
            canvas.drawBitmapMesh(art,COLS,ROWS,vertices,0,null,0,bitmapPaint);
        } else canvas.drawBitmap(art,0,0,bitmapPaint);
        if(poseAmount==0) {
            canvas.save();
            canvas.translate(0,character.torsoDy);
            canvas.rotate(character.torsoDegrees,CharacterMotion.TORSO_X,CharacterMotion.TORSO_Y);
            canvas.translate(character.headDx,character.headDy);
            canvas.rotate(character.headDegrees,CharacterMotion.HEAD_X,CharacterMotion.HEAD_Y);
            drawExpressions(canvas);
            canvas.restore();
        }
        canvas.restore();
        if(settings.particles) ambient(canvas,w,h,mx,my);
        touchEffects(canvas,w,h);
    }

    private void torsoTransform(Canvas canvas) {
        canvas.translate(0,character.torsoDy);
        canvas.rotate(character.torsoDegrees,CharacterMotion.TORSO_X,CharacterMotion.TORSO_Y);
    }

    private void headTransform(Canvas canvas) {
        torsoTransform(canvas);
        canvas.translate(character.headDx,character.headDy);
        canvas.rotate(character.headDegrees,CharacterMotion.HEAD_X,CharacterMotion.HEAD_Y);
    }

    private void drawLayered(Canvas canvas,float amount,float age,float gust) {
        // The underlay contains no moving head or glove, so opening a gap never stretches
        // nearby outlines or leaves a second copy of a finger behind.
        for(int row=0;row<=ROWS;row++) for(int col=0;col<=COLS;col++) {
            int i=row*(COLS+1)+col;
            float x=col*art.getWidth()/(float)COLS,y=row*art.getHeight()/(float)ROWS;
            character.deform(x,y,0,0,0,torsoWeights[i],posePin[i],vertices,i*2);
        }
        canvas.drawBitmapMesh(layers.base,COLS,ROWS,vertices,0,null,0,bitmapPaint);
        float wind=settings.motion ? art.getWidth()*.04f*settings.strength*(1+gust*.25f) : 0;
        hairMotion.update(time,wind,amount,followX);
        for(int row=0;row<=ROWS;row++) for(int col=0;col<=COLS;col++) {
            int i=row*(COLS+1)+col;
            hairMotion.deform(col*art.getWidth()/(float)COLS,row*art.getHeight()/(float)ROWS,
                    layeredHair[i]*pin[i],vertices,i*2);
        }
        canvas.save(); headTransform(canvas);
        canvas.drawBitmapMesh(layers.head,COLS,ROWS,vertices,0,null,0,bitmapPaint); canvas.restore();
        // Back hair stays behind the shoulder. The front locks are then painted above it.
        canvas.save(); torsoTransform(canvas); canvas.clipPath(bodyOcclusion);
        canvas.drawBitmap(layers.base,0,0,bitmapPaint); canvas.restore();
        canvas.save(); headTransform(canvas); canvas.clipPath(frontHead);
        canvas.drawBitmapMesh(layers.head,COLS,ROWS,vertices,0,null,0,bitmapPaint);
        drawExpressions(canvas);
        canvas.restore();
        for(int row=0;row<=ROWS;row++) for(int col=0;col<=COLS;col++) {
            int i=row*(COLS+1)+col;
            float x=col*art.getWidth()/(float)COLS,y=row*art.getHeight()/(float)ROWS;
            // Weights vary along the wrist only, never across a finger's silhouette.
            character.deform(x,y,0,1-SceneMath.smooth(1090,1260,y),
                    1-SceneMath.smooth(1190,1330,y),1,1,vertices,i*2);
        }
        canvas.drawBitmapMesh(layers.glove,COLS,ROWS,vertices,0,null,0,bitmapPaint);
    }

    private void drawExpressions(Canvas canvas) {
        float tapAge=settings.touch && tappedAt>=0 ? (clock.getAsLong()-tappedAt)/1000f : Float.POSITIVE_INFINITY;
        expressions.draw(canvas,time,tapAge,tapAge,settings.expressions);
    }

    private void ambient(Canvas canvas,int w,int h,float mx,float my) {
        fx.setStyle(Paint.Style.FILL);
        float unit=Math.min(w,h);
        for(int i=0;i<18;i++) {
            float a=seeds[i*4], b=seeds[i*4+1], depth=seeds[i*4+2], phase=seeds[i*4+3];
            float x=(a+(float)Math.sin(time*.17f+phase*6.28f)*.025f)*w+mx*unit*.015f*depth;
            float y=((b-time*(.009f+depth*.008f))%1+1)%1*h+my*unit*.018f*depth;
            // Keep the face clear: airy, sparse particles in the surrounding space.
            float alpha=(.45f+.55f*(float)Math.pow(Math.sin(time*.65f+phase*6.28f),2));
            fx.setColor(i%3==0 ? Color.rgb(175,155,84) : Color.rgb(87,133,109));
            fx.setAlpha((int)(alpha*110));
            float r=unit*(.0022f+depth*.0040f);
            if(i%3==0) leaf(canvas,x,y,r*2,time*.6f+phase*6.28f);
            else canvas.drawCircle(x,y,r,fx);
            if(i%4==0) {
                fx.setStrokeWidth(Math.max(1,unit*.001f));
                canvas.drawLine(x-r*2,y,x+r*2,y,fx);
                canvas.drawLine(x,y-r*2,x,y+r*2,fx);
            }
        }
    }

    private void touchEffects(Canvas canvas,int w,int h) {
        float unit=Math.min(w,h);
        Iterator<Burst> it=bursts.iterator();
        while(it.hasNext()) {
            Burst burst=it.next();
            float age=time-burst.start;
            if(age>2.1f) { it.remove(); continue; }
            float p=age/2.1f, fade=(1-p)*(1-p);
            float x=burst.x*w,y=burst.y*h, radius=unit*(.018f+.27f*p);
            fx.setStyle(Paint.Style.STROKE);
            fx.setStrokeWidth(Math.max(1,unit*.0035f*(1-p)));
            fx.setColor(Color.rgb(30,105,80)); fx.setAlpha((int)(230*fade));
            canvas.drawCircle(x,y,radius,fx);
            fx.setAlpha((int)(120*fade)); canvas.drawCircle(x,y,radius*.75f,fx);
            fx.setStyle(Paint.Style.FILL);
            for(int j=0;j<12;j++) {
                float angle=j*6.283185f/12+burst.start+age*.35f;
                float distance=radius*(.70f+(j%3)*.21f);
                float px=x+(float)Math.cos(angle)*distance;
                float py=y+(float)Math.sin(angle)*distance-age*unit*.025f;
                fx.setColor(j%3==0 ? Color.rgb(185,151,63) : Color.rgb(64,139,117));
                fx.setAlpha((int)(245*fade));
                leaf(canvas,px,py,unit*.011f*(1-p)+1,angle+age);
            }
        }
    }

    private void leaf(Canvas canvas,float x,float y,float r,float angle) {
        canvas.save(); canvas.translate(x,y); canvas.rotate(angle*57.29578f);
        petal.reset(); petal.moveTo(-r,0); petal.quadTo(0,-r*.8f,r,0); petal.quadTo(0,r*.8f,-r,0);
        canvas.drawPath(petal,fx); canvas.restore();
    }

    void release() { art.recycle(); expressions.release(); if(layers!=null) layers.release(); bursts.clear(); }

    private static final class Burst {
        final float x,y,start;
        Burst(float x,float y,float start) { this.x=x; this.y=y; this.start=start; }
    }
}
