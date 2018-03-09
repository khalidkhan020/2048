package com.appzone.game;

import android.app.Application;

import com.google.android.gms.ads.MobileAds;

/**
 * Created by khalid on 3/7/18.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MobileAds.initialize(this, Constants.admobId);
    }
}
