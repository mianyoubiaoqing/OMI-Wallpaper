package io.github.omi.wallpaper;

import android.graphics.Path;
import android.graphics.Paint;

/** The original shoulder silhouette owns its contour; rear hair stays behind it. */
final class ShoulderSurface {
    private static final float[] OUTLINE={475,890,501,865,530,850,550,847,560,787,572,777,
            608,834,633,853,655,875,679,898,702,925,726,962,748,1009,766,1059,
            784,1105,810,1190,837,1277,869,1350,894,1368,898,1410,718,1460,
            485,1310,474,1180,474,1050,460,989,466,939};

    static float originalWeight(float x,float y) {
        if(x<550 || x>900 || y<775 || y>1465) return 0;
        // Left of the exposed shoulder, retain the underpainting behind the front lock.
        return SceneMath.smooth(550,570,x)*SceneMath.smooth(-5,-2,SceneMath.signedDistance(OUTLINE,x,y));
    }

    static void trace(Path path) {
        path.reset(); path.moveTo(OUTLINE[0],OUTLINE[1]);
        for(int i=2;i<OUTLINE.length;i+=2) path.lineTo(OUTLINE[i],OUTLINE[i+1]);
        path.close();
        // Include the ink stroke around the traced contour, not just the skin inside.
        Paint stroke=new Paint(); stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(6); stroke.setStrokeJoin(Paint.Join.ROUND);
        Path border=new Path(); stroke.getFillPath(path,border); path.op(border,Path.Op.UNION);
    }
}
