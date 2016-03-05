package app.ssru.mrsmile.signlanguage;

import android.content.Context;
import android.graphics.Typeface;

/**
 * Created by Mr.Smile on 28/11/2558.
 */
public class FrontBuild {

    public static Typeface CANTERBURY;
    public static Typeface FEFCIT2;

    public FrontBuild(Context mContext) {
        CANTERBURY = Typeface.createFromAsset(mContext.getAssets(), "fonts/canterbury.ttf");
        FEFCIT2 = Typeface.createFromAsset(mContext.getAssets(), "fonts/fefcit2.ttf");
    }
}
