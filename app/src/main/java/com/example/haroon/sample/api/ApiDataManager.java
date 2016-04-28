package com.example.haroon.sample.api;

import com.example.haroon.sample.model.Question;
import com.example.haroon.sample.model.Region;
import com.example.haroon.sample.model.Subject;
import com.example.haroon.sample.model.User;
import com.example.haroon.sample.utility.URLConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashSet;

/**
 * Created by Haroon on 4/3/2016.
 */
public class ApiDataManager implements HttpRequestDelegate {

    private static final String TAG = "ApiDataManager";

    // Singleton
    private static ApiDataManager instance = null;

    // if re-login tried then blocked the coming retries
    // and save these http requespts in waiting set for retry
    private Boolean isReLoginTried = false;
    private LinkedHashSet<HttpRequest> httpRequestForRetrySet = new LinkedHashSet<>();

    public static ApiDataManager getInstance() {
        if (instance == null) {
            instance = new ApiDataManager();
            HttpRequest.initTrust();
        }

        return instance;
    }


    public void usersMe(ApiRequestDelegate requestDelegate){
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.url = URLConstants.USER_URL;
        httpRequest.method = HttpRequest.METHOD_GET;
        httpRequest.requestDelegate = requestDelegate;
        httpRequest.delegate = this;

        httpRequest.doRequest();
    }

    public void questionDetail(ApiRequestDelegate requestDelegate) {
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.url = URLConstants.QUESTION_URL;
        httpRequest.method = HttpRequest.METHOD_GET;
        httpRequest.requestDelegate = requestDelegate;
        httpRequest.delegate = this;

        httpRequest.doRequest();
    }

    private ApiResultWithValueMap parseSuccessFailResult(String responseString) {
        ApiResultWithValueMap result = new ApiResultWithValueMap();

        ApiResult apiResult = new ApiResult();
        apiResult.success = false;

        result.apiResult = apiResult;

        try {
            JSONObject jsonObject = new JSONObject(responseString);

            if (jsonObject.has("status")){
                if ("success".equals(jsonObject.getString("status"))) {
                    apiResult.success = true;
                }
            }

            if (!jsonObject.has("message")){

                if (jsonObject.has("data")) {
                    apiResult.success = true;
                    result.valueJson = jsonObject.optJSONObject("data");
                }
                else if (jsonObject.has("id")) {
                    apiResult.success = true;
                    result.valueJson = jsonObject;
                }

                //TODO Error messages
//                if (!apiResult.success && result.valueJson != null){
//                    apiResult.failReason = result.valueJson.optString("reason");
//                }
            }

            return result;
        } catch (JSONException e) {
            return result;
        }
    }

    @Override
    public void requestCompleted(HttpRequest httpRequest) {

        if (httpRequest.requestDelegate != null) {
            ApiResultWithValueMap apiResultWithValueMap = this.parseSuccessFailResult(httpRequest.responseString);

            ApiResult apiResult = apiResultWithValueMap.apiResult;

            if (!apiResult.success) {

                //no need to further parse the result
                httpRequest.requestDelegate.apiCompleted(apiResult, httpRequest);
                return;
            }
            JSONObject valueJson = apiResultWithValueMap.valueJson;

            if (URLConstants.USER_URL.equals(httpRequest.url)){
                apiResult.valueObject = this.parseUsersMe(valueJson);
            }
            else if (URLConstants.QUESTION_URL.equals(httpRequest.url)){
                apiResult.valueObject = this.parseQuestionDetail(valueJson);
            }

            httpRequest.requestDelegate.apiCompleted(apiResult, httpRequest);

        }
    }

    private User parseUsersMe(JSONObject valueJson) {
        if (valueJson == null){
            return null;
        }

        User user = new User();
        user.userID = valueJson.optInt("id");
        user.gender = valueJson.optString("gender");

        JSONObject names = valueJson.optJSONObject("name");
        user.firstName = names.optString("first");
        user.lastName = names.optString("last");

        user.email = valueJson.optString("email");
        user.username = valueJson.optString("username");
        user.registerNo = valueJson.optString("registered");
        user.dob = valueJson.optString("dob");
        user.schoolName = valueJson.optString("school");
        user.countryCode = valueJson.optString("country_code");
        user.phoneNo = valueJson.optString("phone");
        user.profilePicture = valueJson.optString("profile_pic_url");
        //TODO Role
        user.rating = valueJson.optInt("rating");
        user.ratingTotal = valueJson.optInt("rating_total");

        return user;
    }

    private Question parseQuestionDetail(JSONObject valueJson) {
        if (valueJson == null){
            return null;
        }

        Question question = new Question();
        question.questionId = valueJson.optInt("id");

        JSONObject askedBy = valueJson.optJSONObject("asked_by");
        question.askedBy = new User();
        question.askedBy.userID = askedBy.optInt("id");
        question.askedBy.email = askedBy.optString("email");
        question.askedBy.username = askedBy.optString("username");

        //TODO Role asked
        question.askedBy.profilePicture = askedBy.optString("profile_pic_url");

        question.description = valueJson.optString("description");
        question.status = valueJson.optString("status");
        question.userId = valueJson.optInt("user_id");
        question.answeredTutorId = valueJson.optInt("answer_tutor_id");

        JSONObject answeredBy = valueJson.optJSONObject("answered_by");
        question.answeredBy = new User();
        question.answeredBy.userID = answeredBy.optInt("id");
        question.answeredBy.email = answeredBy.optString("email");
        question.answeredBy.username = answeredBy.optString("username");
        //TODO role answered
        question.answeredBy.profilePicture = answeredBy.optString("profile_pic_url");

        question.creationDate = valueJson.optString("created_at");

        JSONObject subject = valueJson.optJSONObject("subject");
        question.subject = new Subject();
        question.subject.subjectId = subject.optInt("id");
        question.subject.abbr = subject.optString("abbr");
        question.subject.description = subject.optString("description");

        JSONObject region = subject.optJSONObject("region");
        question.subject.region = new Region();
        question.subject.region.regionId = region.optInt("id");
        question.subject.region.name = region.optString("name");
        question.subject.region.fullName = region.optString("full_name");

        question.pictureUrl = valueJson.optString("picture_url");
        question.userRating = valueJson.optDouble("user_rating");

        return question;
    }

    private class ApiResultWithValueMap{
        public ApiResult apiResult;
        public JSONObject valueJson;
    }
}

