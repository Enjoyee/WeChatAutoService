package com.glimmer.wechatautoservice;

import android.app.Application;

/**
 * @author Glimmer
 * 2019/01/18
 */
public class BaseApp extends Application {
    private static BaseApp INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
    }

    public static BaseApp getInstance() {
        return INSTANCE;
    }

}
