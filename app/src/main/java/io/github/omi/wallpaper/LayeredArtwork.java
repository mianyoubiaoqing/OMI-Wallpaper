package io.github.omi.wallpaper;
import android.content.Context;
import android.graphics.Bitmap;
import java.io.IOException;

/** Reference-counted immutable layers shared by preview and wallpaper engines. */
final class LayeredArtwork {
    private static LayeredArtwork cached;
    private int references;
    final Bitmap base,head,glove;
    static synchronized LayeredArtwork acquire(Context context,Bitmap original) {
        if(cached==null) cached=new LayeredArtwork(context);
        cached.references++; return cached;
    }
    void release() {
        synchronized(LayeredArtwork.class) {
            if(--references==0) { base.recycle(); head.recycle(); glove.recycle(); cached=null; }
        }
    }
    private LayeredArtwork(Context context) {
        if(!ArtworkAssets.hasPack(context)) {
            base=DemoArtwork.body(); head=DemoArtwork.head(); glove=DemoArtwork.hand(); return;
        }
        Bitmap b=null,h=null,g=null;
        try {
            b=ArtworkAssets.load(context,"body.png"); h=ArtworkAssets.load(context,"head.png");
            g=ArtworkAssets.load(context,"hand.png");
        } catch(IOException error) {
            if(b!=null) b.recycle(); if(h!=null) h.recycle(); if(g!=null) g.recycle();
            throw new IllegalStateException("Unable to load character layers",error);
        }
        base=b; head=h; glove=g;
    }
}
