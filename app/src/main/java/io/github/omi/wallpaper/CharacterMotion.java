package io.github.omi.wallpaper;

/** Four articulated rotations. Rigid face, hand and torso cores taper at their attachments. */
final class CharacterMotion {
    static final float HEAD_X=427,HEAD_Y=890,HAND_X=203,HAND_Y=1135;
    static final float ARM_X=385,ARM_Y=1390,TORSO_X=575,TORSO_Y=1510;
    private static final float[] HEAD={0,570,85,428,160,340,240,272,245,245,317,155,380,32,
            414,32,795,55,790,164,712,270,665,366,649,397,689,543,745,549,819,628,822,771,
            728,827,702,846,594,842,516,848,480,924,468,1012,415,1091,322,1022,362,960,
            371,861,314,873,265,829,240,788,210,761,196,801,196,824,167,861,163,889,
            155,819,127,819,90,817,78,674,0,600};
    private static final float[] HAND={91,823,117,829,150,921,237,837,262,831,276,849,
            247,899,211,955,254,957,296,935,312,934,312,951,277,987,235,1009,226,1082,
            225,1125,257,1198,312,1266,383,1300,392,1417,319,1394,269,1347,216,1282,
            161,1210,128,1150,118,1090,91,1021,61,969,76,957,116,963,124,940,110,900};

    float headDegrees,headDx,headDy,torsoDegrees,torsoDy;
    private float headSin,headCos,handSin,handCos,armSin,armCos,torsoSin,torsoCos;

    static float headWeight(float x,float y) {
        float region=SceneMath.smooth(-36,0,SceneMath.signedDistance(HEAD,x,y));
        float glove=SceneMath.smooth(-18,0,SceneMath.signedDistance(HAND,x,y));
        float attachment=1-SceneMath.smooth(885,1050,y);
        return region*(1-glove)*attachment*SceneMath.smooth(25,60,SceneMath.bodyClearance(x,y));
    }

    static float handWeight(float x,float y) {
        if(x>210 && y<875 && SceneMath.signedDistance(HEAD,x,y)>0) return 0;
        float region=SceneMath.smooth(-24,0,SceneMath.signedDistance(HAND,x,y));
        // Wrist/forearm flex only in this short transition; the elbow and shoulder stay put.
        return region*(1-SceneMath.smooth(1090,1260,y));
    }

    static boolean gloveRegion(float x,float y) {
        return x<430 && y>=810 && y<1350 && SceneMath.signedDistance(HAND,x,y)>-24;
    }

    static boolean forearmInterior(float x,float y) {
        return y>=1125 && y<1350 && SceneMath.signedDistance(HAND,x,y)>0;
    }

    static float armWeight(float x,float y) {
        if(x>210 && y<875 && SceneMath.signedDistance(HEAD,x,y)>0) return 0;
        return SceneMath.smooth(-24,0,SceneMath.signedDistance(HAND,x,y))*(1-SceneMath.smooth(1190,1330,y));
    }

    static float torsoWeight(float x,float y) {
        float body=SceneMath.smooth(-80,-30,-SceneMath.bodyClearance(x,y));
        float head=SceneMath.smooth(-70,-25,SceneMath.signedDistance(HEAD,x,y));
        float hand=SceneMath.smooth(-60,-24,SceneMath.signedDistance(HAND,x,y));
        return Math.max(body,Math.max(head,hand))*(1-SceneMath.smooth(1440,1740,y));
    }

    void update(float time,float amount,float handAmount,float followX,float followY,float responseAge) {
        float nod=responseAge>=0 && responseAge<1.5f ?
                (float)Math.sin(responseAge/1.5f*Math.PI)*.20f : 0;
        // All joint curves are periodic over ten seconds, with continuous velocities at
        // the loop boundary. Phase offsets keep the body from moving like a single board.
        float phase=time*(float)(Math.PI*2/10);
        headDegrees=amount*((float)Math.sin(phase)*2.15f+
                (float)Math.sin(phase*2)*.24f+followX*.22f+nod);
        headDx=amount*(float)Math.sin(phase)*1.5f;
        headDy=amount*((float)Math.sin(phase)*1.1f+followY*.3f);
        float headRadians=headDegrees*(float)Math.PI/180;
        headSin=(float)Math.sin(headRadians); headCos=(float)Math.cos(headRadians);
        // The hand follows roughly half a second later, instead of moving in lockstep.
        float handDegrees=amount*handAmount*((float)Math.sin(phase-.42f)*8.5f+
                (float)Math.sin(phase*2-.25f)*.75f+followX*.5f);
        float handRadians=handDegrees*(float)Math.PI/180;
        handSin=(float)Math.sin(handRadians); handCos=(float)Math.cos(handRadians);
        float armRadians=amount*handAmount*((float)Math.sin(phase-.25f)*1.55f+(float)Math.sin(phase*2)*.18f)*(float)Math.PI/180;
        armSin=(float)Math.sin(armRadians); armCos=(float)Math.cos(armRadians);
        torsoDegrees=amount*(float)Math.sin(phase-.6f)*.34f;
        torsoDy=amount*(float)Math.sin(phase-.6f)*.8f;
        float torsoRadians=torsoDegrees*(float)Math.PI/180;
        torsoSin=(float)Math.sin(torsoRadians); torsoCos=(float)Math.cos(torsoRadians);
    }

    float headX(float x,float y) { return HEAD_X+(x-HEAD_X)*headCos-(y-HEAD_Y)*headSin+headDx; }
    float headY(float x,float y) { return HEAD_Y+(x-HEAD_X)*headSin+(y-HEAD_Y)*headCos+headDy; }

    void deform(float x,float y,float headWeight,float handWeight,float armWeight,float torsoWeight,
                float pin,float[] target,int offset) {
        float hx=headX(x,y)-x,hy=headY(x,y)-y;
        float wx=(x-HAND_X)*(handCos-1)-(y-HAND_Y)*handSin;
        float wy=(x-HAND_X)*handSin+(y-HAND_Y)*(handCos-1);
        float px=x+hx*headWeight+wx*handWeight,py=y+hy*headWeight+wy*handWeight;
        float ax=(px-ARM_X)*(armCos-1)-(py-ARM_Y)*armSin;
        float ay=(px-ARM_X)*armSin+(py-ARM_Y)*(armCos-1);
        px+=ax*armWeight; py+=ay*armWeight;
        float tx=(px-TORSO_X)*(torsoCos-1)-(py-TORSO_Y)*torsoSin;
        float ty=(px-TORSO_X)*torsoSin+(py-TORSO_Y)*(torsoCos-1)+torsoDy;
        target[offset]=x+(px+tx*torsoWeight-x)*pin;
        target[offset+1]=y+(py+ty*torsoWeight-y)*pin;
    }
}
