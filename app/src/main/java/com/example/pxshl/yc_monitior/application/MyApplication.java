package com.example.pxshl.yc_monitior.application;

import android.app.Application;
import android.content.Context;

/**
 * Created by pxshl on 17-12-2.
 */

public class MyApplication extends Application {


    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    public static Context getContext(){
        return context;
    }
}
