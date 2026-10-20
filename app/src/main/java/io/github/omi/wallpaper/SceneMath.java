package io.github.omi.wallpaper;

/** Pure geometry: shared by the preview and wallpaper, independently testable. */
public final class SceneMath {
    private SceneMath() {}

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float smooth(float from, float to, float t) {
        float p = clamp((t - from) / (to - from), 0, 1);
        return p * p * (3 - 2 * p);
    }

    public static float bell(float x, float y, float cx, float cy, float rx, float ry) {
        float a = (x-cx)/rx, b = (y-cy)/ry;
        return (float) Math.exp(-2 * (a*a+b*b));
    }

    // Hand-traced regions in original artwork coordinates. Never use a broad body-area
    // Gaussian for hair: it also displaces the shoulder/back beneath the flowing strands.
    private static final float[] UPPER_HAIR={655,831,736,826,830,869,950,909,1040,923,1140,900,1240,830,
            1240,1032,1140,1030,1020,1021,900,1007,820,986,742,936,672,888};
    private static final float[] LOWER_HAIR={710,970,815,1020,900,1090,1020,1155,1140,1182,1240,1165,
            1240,1351,1135,1302,1020,1281,905,1220,822,1130,754,1040};
    private static final float[] FRONT_HAIR={462,700,555,700,568,822,538,877,487,945,476,1010,
            448,1069,401,1097,418,1060,444,998,455,927,450,854,455,785};
    private static final float[] LEFT_HAIR={118,535,173,522,165,610,159,688,166,741,179,799,
            154,837,117,794,103,739,102,662};
    private static final float[] BODY={518,837,593,837,631,860,680,910,730,991,765,1058,797,1146,
            844,1282,871,1361,720,1430,466,1290,425,1210,400,1120,415,1065,440,990,459,930,480,869};

    static float bodyClearance(float x,float y) { return -signedDistance(BODY,x,y); }

    public static float hairWeight(float x, float y) {
        float px=x*1240,py=y*1754;
        float bodyClearance=-signedDistance(BODY,px,py);
        if(bodyClearance<=28 || (px>180 && px<459 && py<897)) return 0;
        float upper=smooth(-18,22,signedDistance(UPPER_HAIR,px,py))*smooth(650,1120,px);
        float lower=smooth(-16,20,signedDistance(LOWER_HAIR,px,py))*smooth(730,1180,px);
        float front=smooth(-8,18,signedDistance(FRONT_HAIR,px,py))*smooth(700,1080,py)*.55f;
        float left=smooth(-8,14,signedDistance(LEFT_HAIR,px,py))*smooth(535,760,py)*(1-smooth(760,827,py))*.65f;
        return Math.max(Math.max(upper,lower),Math.max(front,left))*smooth(28,62,bodyClearance);
    }

    /** Back locks only. The two front locks are articulated by HairMotion. */
    static float layeredHairWeight(float x,float y) {
        float px=x*1240,py=y*1754;
        float upper=smooth(-32,8,signedDistance(UPPER_HAIR,px,py))*smooth(650,1080,px);
        float lower=smooth(-30,8,signedDistance(LOWER_HAIR,px,py))*smooth(730,1120,px);
        return Math.max(upper,lower);
    }

    static float signedDistance(float[] polygon,float x,float y) {
        boolean inside=false;
        float distance=Float.MAX_VALUE;
        for(int i=0,j=polygon.length-2;i<polygon.length;j=i,i+=2) {
            float ax=polygon[j],ay=polygon[j+1],bx=polygon[i],by=polygon[i+1];
            if((ay>y)!=(by>y) && x<(bx-ax)*(y-ay)/(by-ay)+ax) inside=!inside;
            float dx=bx-ax,dy=by-ay;
            float t=clamp(((x-ax)*dx+(y-ay)*dy)/(dx*dx+dy*dy),0,1);
            float ex=x-ax-dx*t,ey=y-ay-dy*t;
            distance=Math.min(distance,(float)Math.sqrt(ex*ex+ey*ey));
        }
        return inside ? distance : -distance;
    }

    public static float edgePin(float x, float y) {
        return smooth(0,.045f,x)*(1-smooth(.955f,1,x))*smooth(0,.03f,y)*(1-smooth(.97f,1,y));
    }

    public static float coverScale(int w, int h, int iw, int ih, float zoom) {
        return Math.max((float) w/iw, (float) h/ih) * Math.max(1, zoom) * 1.04f;
    }

    public static float offset(int viewport, float content, float framing, float shift) {
        // Movement can never reveal an empty strip, including extreme framing.
        return clamp((viewport-content)*clamp(framing,0,1)+shift, viewport-content, 0);
    }
}
