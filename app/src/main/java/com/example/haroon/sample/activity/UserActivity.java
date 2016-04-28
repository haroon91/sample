package com.example.haroon.sample.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.haroon.sample.MyApp;
import com.example.haroon.sample.R;
import com.example.haroon.sample.api.ApiResult;
import com.example.haroon.sample.api.ApiResultExecutor;
import com.example.haroon.sample.api.HttpRequest;
import com.example.haroon.sample.cache.DataManager;
import com.example.haroon.sample.cache.LocalRequestDelegate;
import com.example.haroon.sample.model.User;
import com.example.haroon.sample.utility.AppConstants;
import com.example.haroon.sample.utility.URLConstants;
import com.example.haroon.sample.utility.Utility;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserActivity extends MyBaseActivity {

    private User mUser;

    private Bitmap userPicBitmap;

    private TextView name, username, dob, education, email, rating;
    private CircleImageView userImage;
    private ImageView gender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        if (!DataManager.dbInitialized) {
            DataManager.initCachedDb(this);
        }

        Intent intent = getIntent();
        if (intent.getData() != null) {

            if (!DataManager.dbInitialized) {
                DataManager.initCachedDb(this);
            }

            Uri data = intent.getData();
            String pathPrefix = data.toString();
            if (pathPrefix.length() > 25) {
                pathPrefix = pathPrefix.substring(25);
                URLConstants.USER_URL = "https://api.myjson.com/bins/" + pathPrefix;
                URLConstants.USER_TAG = pathPrefix;
                MyApp.intentTag = true;
            }
        }
        else {
            MyApp.intentTag = false;
        }

        this.setPageTag(AppConstants.Pages.USER_PAGE);

        dataRequestInfoBundle.putString("USER_TAG", URLConstants.USER_TAG);

        apiResultExecutor = new ApiResultExecutor() {
            @Override
            public void execute(ApiResult apiResult, HttpRequest httpRequest) {
                if (!apiResult.success) {
                    new AlertDialog.Builder(UserActivity.this)
                            .setTitle("Invalid response")
                            .setMessage("Please enter a correct link")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    return;
                                }
                            })
                            .show();
                    return;
                }

                if (URLConstants.USER_URL.equals(httpRequest.url)) {


                    mUser = (User) apiResult.valueObject;

                    if (mUser != null) {
                        DataManager.getInstance().saveLocalData(dataRequestInfoBundle, getPageTag(), mUser);

                        ImageLoader.getInstance().loadImage(mUser.profilePicture, new SimpleImageLoadingListener() {
                                    @Override
                                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                        userPicBitmap = loadedImage;
                                        DataManager.getInstance().saveUserImage(userPicBitmap, String.valueOf(mUser.username + ".jpg"));
                                    }
                                }
                        );

                        processData();
                    }
                }
            }
        };

        localRequestDelegate = new LocalRequestDelegate() {
            @Override
            public void localCompleted(Object data) {
                mUser = (User) data;

                if (mUser != null) {
                    processData();
                }
            }
        };

        name = (TextView) findViewById(R.id.tv_name);
        username = (TextView) findViewById(R.id.tv_username);
        dob = (TextView) findViewById(R.id.tv_dob);
        education = (TextView) findViewById(R.id.tv_uni);
        email = (TextView) findViewById(R.id.tv_email);
        userImage = (CircleImageView) findViewById(R.id.civ_user_icon);
        gender = (ImageView) findViewById(R.id.iv_gender);
        rating = (TextView) findViewById(R.id.tv_score);

    }

    @Override
    public void processData() {
        String fullName = mUser.firstName + " " + mUser.lastName;
        name.setText(fullName);

        username.setText(String.format("Username: %s", mUser.username));
        dob.setText(getDate(Long.valueOf(mUser.dob)));
        education.setText(mUser.schoolName);
        email.setText(mUser.email);
        ImageLoader.getInstance().displayImage(mUser.profilePicture, userImage, Utility.displayImageOptions);
        if (mUser.gender.equals("male")){
            gender.setImageResource(R.drawable.user_male);
        }
        else {
            gender.setImageResource(R.drawable.user_female);
        }
        rating.setText(String.format("%.1f / %.1f",mUser.rating,mUser.ratingTotal));
    }

    private String getDate(long millis) {

        SimpleDateFormat formatter = new SimpleDateFormat("dd-mm-yyyy");

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);

        return formatter.format(calendar.getTime());
    }
}
