package com.example.pxshl.yc_monitior.model;

import android.graphics.Bitmap;

/**
 * Created by pxshl on 17-10-3.
 *
 */

public class AlarmInfo {

    private Bitmap mBitmap;
    private String mTime;

    public AlarmInfo(Bitmap bitmap, String time) {
        mBitmap = bitmap;
        mTime = time;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public String getTime() {
        return mTime;
    }
}
