package com.example.haroon.sample.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.example.haroon.sample.MyApp;
import com.example.haroon.sample.api.ApiDataManager;
import com.example.haroon.sample.api.ApiRequestDelegate;
import com.example.haroon.sample.model.Question;
import com.example.haroon.sample.model.Subject;
import com.example.haroon.sample.model.User;
import com.example.haroon.sample.utility.AppConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by Haroon on 4/3/2016.
 */
public class DataManager {

    private static final String TAG = "DataManager";
    private static final String PERMANENT_QUESTION_FILE = "questionImage.jpg";
    private static final String DATABASE_NAME = "DataDictionary";

    private static File m_questionImage;
    private static File m_userImage;

    public static boolean dbInitialized;

    private Context mContext;

    private static DataDictionaryHelper dBInstance = null;

    private final long RESET_MS = 900000; // 15mins

    private long m_lastRequestOnResetableLocalDataTimeStamp = System.currentTimeMillis();

    private final HashSet RESETABLE_LOCAL_DATE_PAGES_SET = new HashSet<String>(Arrays.asList(
            new String[]{AppConstants.Pages.QUESTION_PAGE,
                        AppConstants.Pages.USER_PAGE}));

    private static DataManager instance = null;

    public static DataManager getInstance() {
        if (instance == null){
            instance = new DataManager();
        }
        return instance;
    }

    public static void initCachedDb(Context context) {
        dBInstance = DataDictionaryHelper.getInstance(context);
        dbInitialized = true;
    }

    public boolean containsQuestion(Context context) {
//        if (mQuestion == null){
//            return false;
//        }

        return DataDictionaryHelper.getInstance(context).getQuestion() != null;

    }

    public boolean containsUser (Context context) {
        return DataDictionaryHelper.getInstance(context).getUser() != null;

    }


    public void saveLocalData(Bundle bundle, String pageTag, Object data) {

        switch (pageTag) {
            case AppConstants.Pages.QUESTION_PAGE:

                //add to db
                addQuestionRecordIntoDb((Question) data);
                break;

            case AppConstants.Pages.USER_PAGE:

                addUserRecordIntoDb((User) data);
                break;
        }

    }

    public void requestViewData(Bundle bundle, String pageTag, ApiRequestDelegate apiRequestDelegate, LocalRequestDelegate localRequestDelegate, Context context) {
        // Save last activity time
        this.mContext = context;

        if (this.isResetableLocalData(pageTag)) {
            m_lastRequestOnResetableLocalDataTimeStamp = System.currentTimeMillis();
        }

        if(isLocalDataAvailable(bundle, pageTag, context) && !MyApp.intentTag) {
            this.requestLocalData(bundle, pageTag, localRequestDelegate);
        }
        else {
            if (isNetworkAvailable(context)) {
                this.requestApiData(bundle, pageTag, apiRequestDelegate);
            }
            else {
                Toast.makeText(context, "Network is offline", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Is it part of ResetableLocalData
    private boolean isResetableLocalData(String p_pageTag) {
        return (RESETABLE_LOCAL_DATE_PAGES_SET.contains(p_pageTag) == true);
    }

    private boolean isLocalDataAvailable(Bundle bundle, String pageTag, Context context) {
        switch (pageTag) {

            /*** Activity ***/
            case AppConstants.Pages.QUESTION_PAGE:
                return containsQuestion(context);

            case AppConstants.Pages.USER_PAGE:
                return containsUser(context);

            default:
                return false;
        }
    }

    public void requestLocalData(Bundle bundle, String pageTag, LocalRequestDelegate localRequestDelegate) {
        switch (pageTag) {

            /*** Activity ***/

            case AppConstants.Pages.QUESTION_PAGE:
                localRequestDelegate.localCompleted(getQuestionData());
                break;

            case AppConstants.Pages.USER_PAGE:
                localRequestDelegate.localCompleted(getUserData());
                break;

        }
    }

    public void requestApiData (Bundle bundle, String pageTag, ApiRequestDelegate apiRequestDelegate) {

        switch (pageTag) {

            case AppConstants.Pages.QUESTION_PAGE:
                ApiDataManager.getInstance().questionDetail(apiRequestDelegate);
                break;

            case AppConstants.Pages.USER_PAGE:
                ApiDataManager.getInstance().usersMe(apiRequestDelegate);
                break;
        }
    }

    // Return Question data
    public Question getQuestionData () {
//        if (mQuestion == null){
//            return null;
//        }

        if (DataDictionaryHelper.getInstance(mContext).getQuestion() == null){
            return null;
        }


//        return mQuestion;
        return DataDictionaryHelper.getInstance(mContext).getQuestion();
    }

    public User getUserData () {
        if (DataDictionaryHelper.getInstance(mContext).getUser() == null){
            return null;
        }

        return DataDictionaryHelper.getInstance(mContext).getUser();
    }

    public void resetQuestionData() {
        dBInstance.deleteQuestionRecord();
    }

    public void addQuestionRecordIntoDb(Question question) {
        dBInstance.insertQuestionRecords(question);
    }

    public void resetUserData() {
        dBInstance.deleteUserRecord();
    }

    public void addUserRecordIntoDb(User user) {
        dBInstance.insertUserRecords(user);
    }


    /********** DATA DICTIONARY SQLITE DATABASE **********/
    private static class DataDictionaryHelper extends SQLiteOpenHelper {

        private static final int DATABASE_VERSION = 1;
        private static final String TABLE_QUESTIONS = "Questions";
        private static final String TABLE_USERS = "Users";


        //Table - Question Column names
        private static final String KEY_QUESTION_ID = "questionId";
        private static final String DESCRIPTION = "description";
        private static final String STATUS = "status";
        private static final String USER_ID = "userId";
        private static final String USER_EMAIL = "userEmail";
        private static final String USER_USERNAME = "userUsername";
        private static final String USER_PICTURE = "userPicture";

        private static final String TUTOR_ID = "tutorId";
        private static final String TUTOR_EMAIL = "tutorEmail";
        private static final String TUTOR_USERNAME = "tutorUsername";
        private static final String TUTOR_PICTURE = "tutorPicture";

        private static final String CREATION_DATE = "creationDate";
        private static final String USER_RATING = "userRating";

        private static final String SUBJECT_ID = "subjectId";
        private static final String SUBJECT_ABBR = "subjectAbbr";
        private static final String SUBJECT_DESCRIPTION = "subjectDescription";
        private static final String QUESTION_PICTURE = "questionPicture";

        //Table - User Column names
        private static final String KEY_USER_ID = "userId";
        private static final String GENDER = "gender";
        private static final String FIRST_NAME = "firstName";
        private static final String LAST_NAME = "lastName";
        private static final String EMAIL = "email";
        private static final String USERNAME = "username";
        private static final String REGISTER_NO = "registerNo";
        private static final String DOB = "dob";
        private static final String SCHOOL = "school";
        private static final String COUNTRY_CODE = "countryCode";
        private static final String PHONE = "phone";
        private static final String PROFILE_PICTURE = "profilePicture";
        private static final String RATING = "rating";
        private static final String RATING_TOTAL = "ratingTotal";

        //Table - Question creation string
        private static final String CREATE_TABLE_QUESTIONS = "CREATE TABLE "
                + TABLE_QUESTIONS + "(" + KEY_QUESTION_ID + " INTEGER PRIMARY KEY,"
                + DESCRIPTION + " TEXT," + STATUS + " TEXT," + USER_ID + " INTEGER,"
                + USER_EMAIL + " TEXT," + USER_USERNAME + " TEXT," + USER_PICTURE + " TEXT,"
                + TUTOR_ID + " INTEGER," + TUTOR_EMAIL + " TEXT," + TUTOR_USERNAME + " TEXT," + TUTOR_PICTURE + " TEXT,"
                + CREATION_DATE + " TEXT," + USER_RATING + " REAL,"
                + SUBJECT_ID + " INTEGER," + SUBJECT_ABBR + " TEXT," + SUBJECT_DESCRIPTION + " TEXT,"
                + QUESTION_PICTURE + " TEXT" + ");";

        private static final String CREATE_TABLE_USER = "CREATE TABLE "
                + TABLE_USERS + "(" + KEY_USER_ID + " INTEGER PRIMARY KEY,"
                + GENDER + " TEXT," + FIRST_NAME + " TEXT," + LAST_NAME + " TEXT,"
                + EMAIL + " TEXT," + USERNAME + " TEXT," + REGISTER_NO + " TEXT,"
                + DOB + " TEXT," + SCHOOL + " TEXT," + COUNTRY_CODE + " TEXT," + PHONE + " TEXT,"
                + PROFILE_PICTURE + " TEXT," + RATING + " REAL," + RATING_TOTAL + " REAL" + ");";


        public static DataDictionaryHelper getInstance (Context context) {

            if (dBInstance == null) {
                dBInstance = new DataDictionaryHelper(context.getApplicationContext());
            }
            return dBInstance;
        }

        public DataDictionaryHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_QUESTIONS);
            db.execSQL(CREATE_TABLE_USER);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUESTIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            onCreate(db);
        }

        public void insertQuestionRecords (Question q) {

            if (q == null){
                return;
            }

            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();

            db.delete(TABLE_QUESTIONS, null, null);

            values.put(KEY_QUESTION_ID, q.questionId);
            values.put(DESCRIPTION, q.description);
            values.put(STATUS, q.status);
            values.put(USER_ID, q.userId);
            values.put(USER_EMAIL, q.askedBy.email);
            values.put(USER_USERNAME, q.askedBy.username);
            values.put(USER_PICTURE, q.askedBy.profilePicture);
            values.put(TUTOR_ID, q.answeredTutorId);
            values.put(TUTOR_EMAIL, q.answeredBy.email);
            values.put(TUTOR_USERNAME, q.answeredBy.username);
            values.put(TUTOR_PICTURE, q.answeredBy.profilePicture);
            values.put(CREATION_DATE, q.creationDate);
            values.put(USER_RATING, q.userRating);
            values.put(SUBJECT_ID, q.subject.subjectId);
            values.put(SUBJECT_ABBR, q.subject.abbr);
            values.put(SUBJECT_DESCRIPTION, q.subject.description);
            values.put(QUESTION_PICTURE, q.pictureUrl);

            db.insert(TABLE_QUESTIONS, null, values);
            db.close();
        }

        public Question getQuestion () {
            SQLiteDatabase db = this.getWritableDatabase();

            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_QUESTIONS, null);

            Question question = null;
            if (cursor.moveToFirst()) {
                do {
                    question = new Question();
                    question.askedBy = new User();
                    question.answeredBy = new User();
                    question.subject = new Subject();

                    question.questionId = Integer.parseInt(cursor.getString(0));
                    question.description = cursor.getString(1);
                    question.status =cursor.getString(2);
                    question.userId = Integer.parseInt(cursor.getString(3));
                    question.askedBy.userID = Integer.parseInt(cursor.getString(3));
                    question.askedBy.email = cursor.getString(4);
                    question.askedBy.username = cursor.getString(5);
                    question.askedBy.profilePicture = cursor.getString(6);
                    question.answeredTutorId = Integer.parseInt(cursor.getString(7));
                    question.answeredBy.userID = Integer.parseInt(cursor.getString(7));
                    question.answeredBy.email = cursor.getString(8);
                    question.answeredBy.username = cursor.getString(9);
                    question.answeredBy.profilePicture = cursor.getString(10);
                    question.creationDate = cursor.getString(11);
                    question.userRating = Double.parseDouble(cursor.getString(12));
                    question.subject.subjectId = Integer.parseInt(cursor.getString(13));
                    question.subject.abbr = cursor.getString(14);
                    question.subject.description = cursor.getString(15);
                    question.pictureUrl = "file://"+getQuestionImageUri(question.questionId);

                } while (cursor.moveToNext());
            }
            cursor.close();

            return question;
        }

        public void deleteQuestionRecord() {
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUESTIONS);
        }

        public void insertUserRecords (User u) {

            if (u == null){
                return;
            }

            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();

            db.delete(TABLE_USERS, null, null);

            values.put(KEY_USER_ID, u.userID);
            values.put(GENDER, u.gender);
            values.put(FIRST_NAME, u.firstName);
            values.put(LAST_NAME, u.lastName);
            values.put(EMAIL, u.email);
            values.put(USERNAME, u.username);
            values.put(REGISTER_NO, u.registerNo);
            values.put(DOB, u.dob);
            values.put(SCHOOL, u.schoolName);
            values.put(PROFILE_PICTURE, u.profilePicture);
            values.put(COUNTRY_CODE, u.countryCode);
            values.put(PHONE, u.phoneNo);
            values.put(RATING, u.rating);
            values.put(RATING_TOTAL, u.ratingTotal);

            db.insert(TABLE_USERS, null, values);
            db.close();
        }

        public User getUser () {
            SQLiteDatabase db = this.getWritableDatabase();

            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS, null);

            User user = null;
            if (cursor.moveToFirst()) {
                do {
                    user = new User();

                    user.userID = Integer.parseInt(cursor.getString(0));
                    user.gender = cursor.getString(1);
                    user.firstName =cursor.getString(2);
                    user.lastName = cursor.getString(3);
                    user.email = cursor.getString(4);
                    user.username = cursor.getString(5);
                    user.registerNo = cursor.getString(6);
                    user.dob = cursor.getString(7);
                    user.schoolName = cursor.getString(8);
                    user.profilePicture = "file://"+getUserImageUri(user.username);
                    user.countryCode = cursor.getString(10);
                    user.phoneNo  = cursor.getString(11);
                    user.rating  = Double.parseDouble(cursor.getString(12));
                    user.ratingTotal = Double.parseDouble(cursor.getString(13));

                } while (cursor.moveToNext());
            }
            cursor.close();

            return user;
        }

        public void deleteUserRecord() {
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        }
    }


    private static File getPermanentFile(String filename) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File file = new File(Environment.getExternalStorageDirectory(), filename);
            try {
                file.createNewFile();
            } catch (IOException ignored) {
            }

            return file;
        } else {
            return null;
        }
    }

    public void saveQuestionImage(Bitmap questionBitmap, String imageFile) {

        if (questionBitmap != null){
            try {
                // Create file and link it to attribute
                m_questionImage = getPermanentFile(imageFile);

                // Write the bytes in file
                FileOutputStream fos = new FileOutputStream(m_questionImage);
                questionBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos); // Max quality is 100 - It is re-compressed by paperclip

                // Clean
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveUserImage(Bitmap userPicBitmap, String imageFile) {

        if (userPicBitmap != null) {
            try {
                m_userImage = getPermanentFile(imageFile);

                FileOutputStream fos = new FileOutputStream(m_userImage);
                userPicBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);

                fos.flush();
                fos.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public static String getQuestionImageUri (int questionId) {
        String filePath = Environment.getExternalStorageDirectory().getPath();
        filePath += String.format("/%d.jpg",questionId);

        return filePath;
    }

    public static String getUserImageUri (String username) {
        String filePath = Environment.getExternalStorageDirectory().getPath();
        filePath += "/"+username+".jpg";

        return filePath;
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Reset data - Reset if in the background and did not view any related page OR if the region is changed
    public void resetContentLocalDataIfNeeded() {
        long currentTimeStamp = System.currentTimeMillis();
        if ( ( (currentTimeStamp - m_lastRequestOnResetableLocalDataTimeStamp) >= RESET_MS )) {
            Log.d(TAG, "resetContentLocalDataIfNeeded entered if");
            this.resetContentLocalData();
        }
    }

    public void resetContentLocalData() {
        resetQuestionData();
        resetUserData();
    }

}
