package io.github.omi.wallpaper;

/** Continuous eyelid timing with a twenty-second idle cycle. */
final class BlinkMotion {
    static final float PERIOD=20f, WINK_START=7.95f, BLINK_START=17.93f;
    private BlinkMotion() {}

    static float closure(float time,float responseAge,boolean leftEye) {
        if(responseAge>=0 && responseAge<2.1f) return atAge(responseAge-.06f,false);
        float cycle=time%PERIOD;
        float start=cycle<WINK_START+1.42f ? WINK_START : BLINK_START;
        boolean wink=start==WINK_START;
        // Skip a scheduled event in full if a touch response overlapped its start.
        float lastTouch=time-responseAge,absoluteStart=time-cycle+start;
        if(lastTouch>absoluteStart-2.1f && lastTouch<absoluteStart+(wink?1.42f:.84f)) return 0;
        if(wink && !leftEye) return 0;
        return atAge(cycle-start,wink);
    }

    private static float atAge(float age,boolean wink) {
        if(age<0 || age>=(wink?1.42f:.84f)) return 0;
        float close=wink?.35f:.23f,holdEnd=wink?1f:.43f,end=wink?1.42f:.84f;
        if(age<close) return ease(age/close);
        if(age<holdEnd) return 1;
        return 1-ease((age-holdEnd)/(end-holdEnd));
    }

    private static float ease(float t) { return t*t*(3-2*t); }
}
