package io.github.omi.wallpaper;

/** The layered hair's deformation, shared by the renderer and geometric regression checks. */
final class HairMotion {
    private float time,wind,followX,trailing,leftAngle,rightAngle;

    void update(float time,float wind,float amount,float followX) {
        this.time=time; this.wind=wind; this.followX=followX;
        trailing=(float)Math.sin((time-.24f)*.6283185f)*amount*8;
        float lag=(float)Math.sin((time-.24f)*.6283185f)*amount*.6f;
        float breeze=wind/49.6f;
        leftAngle=(float)Math.toRadians((Math.sin(time*1.35-.6)*6.7+
                Math.sin(time*1.9+.4)*.8+followX*.8)*breeze+lag);
        rightAngle=(float)Math.toRadians((Math.sin(time*1.35-1.1)*6.7+
                Math.sin(time*1.9-.2)*.8+followX*.8)*breeze+lag);
    }

    // Convenience path for geometry checks; the renderer caches the polygon weights.
    static void deform(float px,float py,float time,float wind,float amount,float followX,float[] out,int offset) {
        HairMotion motion=new HairMotion(); motion.update(time,wind,amount,followX);
        float x=px/1240f,y=py/1754f;
        motion.deform(px,py,SceneMath.layeredHairWeight(x,y)*SceneMath.edgePin(x,y),out,offset);
    }

    void deform(float px,float py,float backWeight,float[] out,int offset) {
        out[offset]=px; out[offset+1]=py;
        if(backWeight>0) {
            float x=px/1240f,y=py/1754f;
            float wave=(float)Math.sin(time*1.35f-x*2.4f-y*.6f);
            float secondary=(float)Math.sin(time*1.9f-x*3.1f+y*.6f);
            out[offset]+=(wave*.4f+secondary*.12f+followX*.15f)*wind*backWeight;
            out[offset+1]+=((wave+secondary*.16f)*wind+trailing)*backWeight;
        }
        if(py<=740 || px>=635) return;
        // Root follows the head. Bend only the connecting section; every painted
        // point below it shares one rotation, so thin tips keep their width/length.
        // The blend between locks crosses transparent space, never an opaque tip.
        float side=SceneMath.smooth(280,335,px);
        float pivotX=180+(505-180)*side;
        // Above the jaw, only the hair outside the face can bend. Broad smooth margins
        // keep the original face fixed without the old discontinuous vertical boundary.
        float outsideFace=Math.max(1-SceneMath.smooth(100,225,px),SceneMath.smooth(460,525,px));
        float belowFace=SceneMath.smooth(875,965,py);
        float clearance=outsideFace+(1-outsideFace)*belowFace;
        float angle=(leftAngle+(rightAngle-leftAngle)*side)*SceneMath.smooth(740,1005,py)*clearance*
                (1-SceneMath.smooth(575,635,px));
        float dx=px-pivotX,dy=py-740;
        float sin=(float)Math.sin(angle),cos=(float)Math.cos(angle);
        out[offset]+=dx*(cos-1)-dy*sin;
        out[offset+1]+=dx*sin+dy*(cos-1);
    }
}
