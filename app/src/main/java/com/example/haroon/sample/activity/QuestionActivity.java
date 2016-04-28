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
import com.example.haroon.sample.model.Question;
import com.example.haroon.sample.utility.AppConstants;
import com.example.haroon.sample.utility.URLConstants;
import com.example.haroon.sample.utility.Utility;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class QuestionActivity extends MyBaseActivity {

    private static final long ONE_HOUR = 3600000;
    private static final long HOURS_24 = 86400000;
    private static final long ONE_WEEK = 604800000;

    private Bitmap questionBitmap;

    private Question m_question;

    private TextView questionId, qDescription, qSubject, askTime, askUser, answerUser, rating, region;
    private ImageView questionImage;
    private CircleImageView askUserIcon, answerUserIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

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
            if (pathPrefix.length() > 29) {
                pathPrefix = pathPrefix.substring(29);
                URLConstants.QUESTION_URL = "https://api.myjson.com/bins/" + pathPrefix;
                URLConstants.QUESTION_TAG = pathPrefix;
                MyApp.intentTag = true;
            }
        }
        else {
           MyApp.intentTag = false;
        }

        this.setPageTag(AppConstants.Pages.QUESTION_PAGE);

        dataRequestInfoBundle.putString("QUESTION_TAG", URLConstants.QUESTION_TAG);

        apiResultExecutor = new ApiResultExecutor() {
            @Override
            public void execute(ApiResult apiResult, HttpRequest httpRequest) {
                if (!apiResult.success) {
                    new AlertDialog.Builder(QuestionActivity.this)
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

                if (URLConstants.QUESTION_URL.equals(httpRequest.url)) {
                    m_question = (Question) apiResult.valueObject;

                    DataManager.getInstance().saveLocalData(dataRequestInfoBundle, getPageTag(), m_question);

                    ImageLoader.getInstance().loadImage(m_question.pictureUrl, new SimpleImageLoadingListener() {
                                @Override
                                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                    questionBitmap = loadedImage;
                                    DataManager.getInstance().saveQuestionImage(questionBitmap, String.valueOf(m_question.questionId + ".jpg"));
                                }
                            }
                    );

                    processData();
                }
            }
        };

        localRequestDelegate = new LocalRequestDelegate() {
            @Override
            public void localCompleted(Object data) {
                m_question = (Question) data;

                if (m_question != null) {
                    processData();
                }
            }
        };


        questionId = (TextView) findViewById(R.id.tv_qID);
        qDescription = (TextView) findViewById(R.id.tv_questiondesc);
        questionImage = (ImageView) findViewById(R.id.iv_questionimage);
        qSubject = (TextView) findViewById(R.id.tv_subject);
        askTime = (TextView) findViewById(R.id.tv_time);
        askUser = (TextView) findViewById(R.id.tv_ask_username);
        answerUser = (TextView) findViewById(R.id.tv_answer_user);
        rating = (TextView) findViewById(R.id.tv_rating);
        region = (TextView) findViewById(R.id.tv_region);
        askUserIcon = (CircleImageView) findViewById(R.id.civ_asker_icon);
        answerUserIcon = (CircleImageView) findViewById(R.id.civ_answer_icon);

    }

    @Override
    public void processData() {
        //set Views here
        qDescription.setText(m_question.description);

        questionId.setText(String.format("Id: %d", m_question.questionId));
        qSubject.setText(m_question.subject.description);
        askTime.setText(timeDiff(m_question.creationDate));
        askUser.setText(m_question.askedBy.username);
        answerUser.setText(m_question.answeredBy.username);
        rating.setText(String.format("User rating : %.1f", m_question.userRating));

        ImageLoader.getInstance().displayImage(m_question.askedBy.profilePicture, askUserIcon, Utility.displayImageOptions);
        ImageLoader.getInstance().displayImage(m_question.pictureUrl, questionImage, Utility.displayImageOptions);
        ImageLoader.getInstance().displayImage(m_question.answeredBy.profilePicture, answerUserIcon, Utility.displayImageOptions);

    }

    private String timeDiff (String askDate) {

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

        try {
            Date date = formatter.parse(askDate);
            long millis = date.getTime();
            long now = System.currentTimeMillis();

            long difference = now - millis;

            if (difference < HOURS_24){
                int hours = Math.round(difference / ONE_HOUR);
                return String.format("asked %d hours ago",hours);
            }
            else if (difference < ONE_WEEK) {
                int days = Math.round(difference / HOURS_24);
                return String.format("asked %d days ago", days);
            }
            else {
                int weeks = Math.round(difference / ONE_WEEK);
                return String.format("asked %d weeks ago", weeks);
            }

        }
        catch (ParseException e) {
            e.printStackTrace();
        }


        return "";

    }

}
