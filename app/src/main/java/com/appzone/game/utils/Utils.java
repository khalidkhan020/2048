package com.appzone.game.utils;

import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;

/**
 * Created by khalid on 3/9/18.
 */

public class Utils
{
    public static ShapeDrawable drawRoundCornerRectange (Context context, int width, int height, float[] outerRadii, int color) {

        ShapeDrawable rndrect = new ShapeDrawable(new RoundRectShape(outerRadii, null, null));
        rndrect.setIntrinsicHeight(width);
        rndrect.setIntrinsicWidth(height);
        rndrect.getPaint().setColor(color);
        return rndrect;
    }

}
