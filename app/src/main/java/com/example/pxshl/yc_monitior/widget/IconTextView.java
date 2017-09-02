package com.example.pxshl.yc_monitior.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

/**
 * Created by pxshl on 2017/7/29.
 */

public class IconTextView extends AppCompatTextView {
    public IconTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Typeface face = Typeface.createFromAsset(context.getAssets(), "icomoon.ttf");
        setTypeface(face);
    }
}
