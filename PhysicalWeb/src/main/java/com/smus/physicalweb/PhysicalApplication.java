package com.smus.physicalweb;

import android.app.Application;

/**
 * Created by smus on 5/1/14.
 */
public class PhysicalApplication extends Application {

    @Override
    public void onCreate() {
      super.onCreate();

      MetadataResolver.initialize(getApplicationContext());
    }
}
