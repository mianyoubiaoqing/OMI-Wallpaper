package io.github.omi.wallpaper;

import org.junit.Test;
import static org.junit.Assert.*;

public class BlinkMotionTest {
    private float pose(float t,boolean left) { return BlinkMotion.closure(t,Float.POSITIVE_INFINITY,left); }

    @Test public void referenceWinkAndBlinkAreSparseAndRepeatAfterTwentySeconds() {
        for(float t:new float[]{0,2.8f,5,10,12.8f,15,19.5f}) {
            assertEquals(0,pose(t,true),0); assertEquals(0,pose(t,false),0);
        }
        for(float t:new float[]{8.35f,8.6f,8.9f}) {
            assertEquals(1,pose(t,true),0); assertEquals(0,pose(t,false),0);
        }
        assertEquals(1,pose(18.25f,true),0); assertEquals(1,pose(18.25f,false),0);
        for(int i=0;i<200;i++) for(boolean left:new boolean[]{true,false})
            assertEquals(pose(i*.1f+.017f,left),pose(20+i*.1f+.017f,left),.00003f);
    }

    @Test public void eyelidsMoveContinuouslyAtBothFrameRates() {
        for(int fps:new int[]{30,60}) for(float start:new float[]{7.95f,17.93f}) {
            float duration=start==7.95f?.35f:.23f,previous=pose(start,true);
            for(int i=1;i<duration*fps;i++) {
                float current=pose(start+i/(float)fps,true);
                assertTrue("Closing frame held at "+fps+" fps",current>previous);
                assertTrue("Lid jumped",current-previous<.23f);
                previous=current;
            }
        }
    }

    @Test public void touchCannotProduceAHalfFinishedScheduledBlinkAfterItsCooldown() {
        assertEquals(1,BlinkMotion.closure(6.4f,.4f,true),0);
        assertEquals(0,BlinkMotion.closure(8.2f,2.2f,true),0);
        assertEquals(0,BlinkMotion.closure(8.6f,2.6f,true),0);
        assertEquals(1,BlinkMotion.closure(18.25f,12.25f,false),0);
    }
}
