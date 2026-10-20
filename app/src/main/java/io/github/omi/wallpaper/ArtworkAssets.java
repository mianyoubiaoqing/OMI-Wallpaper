package io.github.omi.wallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/** Optional licensed texture pack; the procedural demo needs no image files. */
final class ArtworkAssets {
    static final int WIDTH=1240,HEIGHT=1754;
    private static final String[] REQUIRED={"original.png","body.png","head.png","hand.png","expression-closed.png"};
    private ArtworkAssets() {}
    static boolean hasPack(Context context) {
        try {
            String[] entries=context.getAssets().list("wallpaper");
            List<String> names=entries==null ? java.util.Collections.emptyList() : Arrays.asList(entries);
            boolean any=false,all=true;
            for(String file:REQUIRED) { any|=names.contains(file); all&=names.contains(file); }
            if(any && !all) throw new IllegalStateException("Incomplete wallpaper pack. See docs/ARTWORK.md.");
            return all;
        } catch(IOException e) { throw new IllegalStateException("Unable to inspect wallpaper assets",e); }
    }
    static Bitmap original(Context context) {
        if(!hasPack(context)) return DemoArtwork.original();
        try { return load(context,"original.png"); }
        catch(IOException e) { throw new IllegalStateException("Unable to load artwork",e); }
    }
    static Bitmap load(Context context,String name) throws IOException {
        try(InputStream in=context.getAssets().open("wallpaper/"+name)) {
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inScaled=false; options.inPreferredConfig=Bitmap.Config.ARGB_8888;
            Bitmap bitmap=BitmapFactory.decodeStream(in,null,options);
            if(bitmap==null) throw new IOException("Invalid bitmap: "+name);
            if(bitmap.getWidth()!=WIDTH || bitmap.getHeight()!=HEIGHT) {
                bitmap.recycle(); throw new IOException("Expected 1240 x 1754: "+name);
            }
            return bitmap;
        }
    }
}
