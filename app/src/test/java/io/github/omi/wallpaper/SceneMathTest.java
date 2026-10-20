package io.github.omi.wallpaper;

import org.junit.Test;
import static org.junit.Assert.*;

public class SceneMathTest {
    @Test public void coverDoesNotExposeBlankEdgesAcrossAspectRatios() {
        int[][] screens={{1280,2772},{1080,2400},{1440,3200},{2400,1080},{2208,1840},{720,1600}};
        for(int[] screen:screens) for(float framing:new float[]{0,.34f,.5f,1}) {
            float scale=SceneMath.coverScale(screen[0],screen[1],1240,1754,1);
            float w=1240*scale,h=1754*scale;
            for(float shift:new float[]{-80,0,80}) {
                float x=SceneMath.offset(screen[0],w,framing,shift);
                float y=SceneMath.offset(screen[1],h,.45f,shift);
                assertTrue(x<=0 && y<=0);
                assertTrue(x+w>=screen[0]-.01f && y+h>=screen[1]-.01f);
            }
        }
    }
    @Test public void eyesAndMouthRemainAlmostStationary() {
        assertTrue(SceneMath.hairWeight(.19f,.38f)<.015f);
        assertTrue(SceneMath.hairWeight(.32f,.395f)<.015f);
        assertTrue(SceneMath.hairWeight(.23f,.455f)<.015f);
        assertTrue(SceneMath.hairWeight(.85f,.55f)>.6f);
    }
    @Test public void shoulderAndBackAreRigidEvenAtMaximumAnimation() {
        for(int y=900;y<=1260;y+=10) {
            // Interior and a conservative portion of the exposed body contour.
            int right= y<1050 ? 690 : 760;
            for(int x=520;x<=right;x+=10) assertEquals("Body at "+x+","+y,0,
                    SceneMath.hairWeight(x/1240f,y/1754f),0);
        }
    }
    @Test public void meshBoundaryIsPinnedAndWeightsAreFinite() {
        for(int row=0;row<=52;row++) for(int col=0;col<=36;col++) {
            float x=col/36f,y=row/52f;
            float hair=SceneMath.hairWeight(x,y),pin=SceneMath.edgePin(x,y);
            assertTrue(Float.isFinite(hair) && hair>=0 && hair<=1);
            assertTrue(Float.isFinite(pin) && pin>=0 && pin<=1);
            if(col==0 || col==36 || row==0 || row==52) assertEquals(0,pin,0);
        }
    }
}
