package io.github.omi.wallpaper;

import org.junit.Test;
import static org.junit.Assert.*;

public class HairMotionTest {
    private float[] at(float x,float y,float t,float wind) {
        float[] p=new float[2]; HairMotion.deform(x,y,t,wind,.8f,0,p,0); return p;
    }
    private float distance(float[] a,float[] b) { return (float)Math.hypot(a[0]-b[0],a[1]-b[1]); }

    @Test public void sideLockHasNoAbruptStretchAcrossFaceProtectionBoundary() {
        for(float t=0;t<20;t+=.05f) for(int y=840;y<=930;y+=5) {
            float ratio=distance(at(184,y,t,49.6f),at(186,y,t,49.6f))/2;
            assertTrue("Side lock torn at y="+y+" t="+t+"; local length ratio="+ratio,ratio>.85f && ratio<1.15f);
        }
    }
    @Test public void freeTipsKeepTheirPaintedShapeAtEveryWindPhase() {
        float[][] points={{180,1040,225,1080},{415,1040,450,1100},{440,1030,495,1045}};
        for(float wind:new float[]{22.32f,49.6f,62f}) for(float t=0;t<20;t+=.05f) for(float[] p:points) {
            float before=(float)Math.hypot(p[2]-p[0],p[3]-p[1]);
            float after=distance(at(p[0],p[1],t,wind),at(p[2],p[3],t,wind));
            assertEquals("Free tip squeezed at x="+p[0]+" t="+t,before,after,before*.015f);
        }
    }

    @Test public void connectingMeshDoesNotFoldAtMaximumGust() {
        // Local cells through the opaque root/shaft, including both ends of the bend.
        float[][] bands={{120,260},{385,550}};
        for(float t=0;t<20;t+=.1f) for(float[] band:bands)
            for(float x=band[0];x<band[1];x+=15) for(float y=725;y<1030;y+=15) {
                float[] a=at(x,y,t,62),b=at(x+15,y,t,62),c=at(x,y+15,t,62);
                float area=((b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]))/225;
                assertTrue("Folded/stretched root at "+x+","+y+" t="+t+" area="+area,area>.6f && area<1.4f);
            }
    }

    @Test public void windKeepsFaceFixedAndBothTipsVisiblyMoving() {
        float[][] face={{220,660},{320,735},{300,830},{450,860}};
        for(float t=0;t<20;t+=.1f) for(float[] p:face) assertArrayEquals(p,at(p[0],p[1],t,62),.001f);
        for(float[] tip:new float[][]{{220,1080},{430,1080}}) {
            float min=Float.MAX_VALUE,max=-Float.MAX_VALUE;
            for(float t=0;t<10;t+=.1f) { float x=at(tip[0],tip[1],t,22.32f)[0]; min=Math.min(min,x); max=Math.max(max,x); }
            assertTrue("Tip motion lost at default strength",max-min>12);
        }
    }

    @Test public void sideLocksSwayFromCheekLevelAboveTheOldRoot() {
        for(float[] p:new float[][]{{155,840},{550,840}}) {
            float min=Float.MAX_VALUE,max=-Float.MAX_VALUE;
            for(float t=0;t<10;t+=.1f) { float x=at(p[0],p[1],t,22.32f)[0]; min=Math.min(min,x); max=Math.max(max,x); }
            assertTrue("Upper side lock still pinned at "+p[0]+","+p[1],max-min>2);
        }
    }
}
