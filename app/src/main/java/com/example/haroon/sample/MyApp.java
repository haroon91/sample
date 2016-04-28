package com.example.haroon.sample;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.example.haroon.sample.cache.DataManager;
import com.example.haroon.sample.utility.Utility;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.io.File;

/**
 * Created by Haroon on 5/3/2016.
 */
public class MyApp extends Application {

    private boolean inited = false;

    private Context mContext;

    public static boolean intentTag;

    private Activity currentActivity = null;

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();

        BroadcastReceiver m_tickReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Check it's time tick
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                    // If in background, set all pages for refresh
                    if (isInBackground()) {
                        DataManager.getInstance().resetContentLocalDataIfNeeded();
                        Log.v("RESET", "Data Reset Called");
                    }
                }
            }
        };

        registerReceiver(m_tickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    public void initializeIfNeeded() {
        if (!inited) {

            //init for image loader
            File cacheDir = StorageUtils.getCacheDirectory(this);
            ImageLoaderConfiguration config = (new ImageLoaderConfiguration.Builder(this))
                    .memoryCache(new LruMemoryCache(4 * 1024 * 1024))
                    .memoryCacheSize(4 * 1024 * 1024)
                    .diskCache(new UnlimitedDiscCache(cacheDir))
                    .diskCacheSize(50 * 1024 * 1024)
                    .diskCacheFileCount(100)
                    .build();
            ImageLoader.getInstance().init(config);

            Utility.displayImageOptions = new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .build();

            inited = true;
        }
    }

    public boolean isInBackground() {
        return ( currentActivity == null );
    }

    public void setCurrentActivity(Activity currentActivity) {
        this.currentActivity = currentActivity;
    }

    public Activity getCurrentActivity() {
        return currentActivity;
    }


}
