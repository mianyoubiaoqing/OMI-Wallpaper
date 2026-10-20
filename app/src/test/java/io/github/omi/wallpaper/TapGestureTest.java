package io.github.omi.wallpaper;

import org.junit.Test;
import static org.junit.Assert.*;

public class TapGestureTest {
    @Test public void rawTapAndLauncherCommandTriggerOnlyOnce() {
        TapGesture tap=new TapGesture(10);
        tap.down(100,100,1000);
        assertFalse(tap.command(1030));
        assertTrue(tap.up(104,103,1100));
        assertFalse(tap.command(1150));
        assertTrue(tap.command(2000));
        assertFalse(tap.command(2100));
    }
    @Test public void dragReturningToStartIsStillADrag() {
        TapGesture tap=new TapGesture(10);
        tap.down(100,100,1000); tap.move(120,100); tap.move(100,100);
        assertFalse(tap.up(100,100,1100));
        assertFalse(tap.command(1150));
    }
    @Test public void longHoldCancelAndOrphanUpAreNotClicks() {
        TapGesture tap=new TapGesture(10);
        tap.down(100,100,1000); assertFalse(tap.up(100,100,1800));
        tap.down(100,100,2000); tap.cancel(2050); assertFalse(tap.up(100,100,2100));
        assertFalse(tap.up(100,100,3000));
        tap.down(100,100,4000); tap.reset(4010); assertFalse(tap.up(100,100,4050));
    }
}
