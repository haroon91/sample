package com.example.haroon.sample.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.example.haroon.sample.MyApp;
import com.example.haroon.sample.api.ApiDataManager;
import com.example.haroon.sample.api.ApiRequestDelegate;
import com.example.haroon.sample.api.ApiResult;
import com.example.haroon.sample.api.ApiResultExecutor;
import com.example.haroon.sample.api.HttpRequest;
import com.example.haroon.sample.cache.DataManager;
import com.example.haroon.sample.cache.LocalRequestDelegate;
import com.example.haroon.sample.utility.AppConstants;

/**
 * Created by Haroon on 4/3/2016.
 */
public abstract class MyBaseActivity extends Activity {

    private MyApp m_myApp;

    // Tag to identify the page
    private String pageTag = AppConstants.Pages.EMPTY;
    // Bundle provides info for data loading
    protected Bundle dataRequestInfoBundle = new Bundle();

    // Api data request delegate
    protected ApiRequestDelegate apiRequestDelegate = new ApiRequestDelegate() {

        @Override
        public void apiCompleted(ApiResult apiResult, HttpRequest httpRequest) {

            if(apiResultExecutor != null)
                apiResultExecutor.execute(apiResult, httpRequest);
        }
    };

    protected ApiDataManager apiDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_myApp = (MyApp) this.getApplication();

        m_myApp.initializeIfNeeded();

        apiDataManager = ApiDataManager.getInstance();

    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d("ActivityLifeCycle", this + " onStart()");

        // Retrieve view data when Activity resumed
        this.retrieveViewData();
    }

    @Override
    protected void onPause() {
        clearReferences();
        super.onPause();

        Log.d("ActivityLifeCycle", this + " onPause()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("ActivityLifeCycle", this + " onResume()");

        m_myApp.setCurrentActivity(this);
    }

    @Override
    protected void onDestroy() {
        clearReferences();
        super.onDestroy();

        Log.d("ActivityLifeCycle", this + " onDestroy()");
    }

    private void clearReferences() {
        Activity currActivity = m_myApp.getCurrentActivity();
        if (currActivity != null && currActivity.equals(this)) {
            m_myApp.setCurrentActivity(null);
        }
    }

    // try to retrieve view data for either cache or api
    private void retrieveViewData() {
        if(apiRequestDelegate != null || localRequestDelegate != null) {
            DataManager.getInstance().requestViewData(dataRequestInfoBundle, pageTag, apiRequestDelegate, localRequestDelegate, this.getApplicationContext());
        } else {
            Log.d("RetrieveViewData", this + " data not retrieved");
        }
    }

    // Api result handler
    protected ApiResultExecutor apiResultExecutor = null;

    // Local data request call back
    protected LocalRequestDelegate localRequestDelegate = null;

    public String getPageTag() {
        return pageTag;
    }

    public void setPageTag(String pageTag) {
        this.pageTag = pageTag;
    }

    // common method for process loaded data
    protected void processData() {
    }

}
