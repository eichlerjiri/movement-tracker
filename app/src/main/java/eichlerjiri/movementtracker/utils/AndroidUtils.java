package eichlerjiri.movementtracker.utils;

import android.content.Context;

public class AndroidUtils {

    public static int spToPix(Context context, float sp) {
        return Math.round(sp * context.getResources().getDisplayMetrics().scaledDensity);
    }
}
