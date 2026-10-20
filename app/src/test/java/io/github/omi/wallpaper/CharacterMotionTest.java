package io.github.omi.wallpaper;

import org.junit.Test;
import static org.junit.Assert.*;

public class CharacterMotionTest {
    private static float[] move(CharacterMotion pose,float x,float y) {
        float[] result=new float[2];
        pose.deform(x,y,CharacterMotion.headWeight(x,y),CharacterMotion.handWeight(x,y),
                CharacterMotion.armWeight(x,y),CharacterMotion.torsoWeight(x,y),1,result,0);
        return result;
    }

    private static float distance(float[] a,float[] b) {
        return (float)Math.hypot(a[0]-b[0],a[1]-b[1]);
    }

    @Test public void backFaceAndFingersKeepTheirShapesThroughEveryPose() {
        float[][] pairs={{540,920,730,1190},{228,672,399,686},{288,804,397,690},{110,850,260,855}};
        CharacterMotion pose=new CharacterMotion();
        for(int step=0;step<=100;step++) {
            pose.update(step*.1f,1,1,0,0,20);
            for(float[] p:pairs) {
                float expected=(float)Math.hypot(p[0]-p[2],p[1]-p[3]);
                assertEquals("Rigid pair at t="+step*.1f,expected,
                        distance(move(pose,p[0],p[1]),move(pose,p[2],p[3])),.01f);
            }
        }
    }

    @Test public void tenSecondIdleLoopsContinuouslyAndHasNoLongHolds() {
        CharacterMotion pose=new CharacterMotion();
        for(float x:new float[]{250,400,700}) {
            pose.update(0,1,1,0,0,20); float[] start=move(pose,x,700);
            pose.update(10,1,1,0,0,20); assertEquals(0,distance(start,move(pose,x,700)),.001f);
        }
        // In every half-second window at least one articulated point changes visibly.
        for(int step=0;step<20;step++) {
            pose.update(step*.5f,1,1,0,0,20);
            float[] head=move(pose,400,650),hand=move(pose,260,850);
            pose.update((step+1)*.5f,1,1,0,0,20);
            assertTrue("Idle hold at "+step*.5f,
                    distance(head,move(pose,400,650))+distance(hand,move(pose,260,850))>.5f);
        }
    }

    @Test public void zeroAmountReturnsOriginalGeometry() {
        CharacterMotion pose=new CharacterMotion();
        pose.update(2.3f,0,1,.8f,-.5f,.4f);
        for(float[] p:new float[][]{{260,850},{400,650},{650,1050}}) {
            float[] moved=move(pose,p[0],p[1]);
            assertArrayEquals(p,moved,.0001f);
        }
    }

    @Test public void strongerHandCanBeAdjustedWithoutMovingTheHead() {
        CharacterMotion pose=new CharacterMotion();
        pose.update(2.5f,1,0,0,0,20);
        float[] head=move(pose,400,650),hand=move(pose,110,850);
        pose.update(2.5f,1,1,0,0,20);
        assertEquals(0,distance(head,move(pose,400,650)),.001f);
        assertTrue(distance(hand,move(pose,110,850))>45);
    }
}
