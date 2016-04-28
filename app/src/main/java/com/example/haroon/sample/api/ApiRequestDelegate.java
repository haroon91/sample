package com.example.haroon.sample.api;

/**
 * Created by Haroon on 4/3/2016.
 */
public interface ApiRequestDelegate {
    void apiCompleted(ApiResult apiResult, HttpRequest httpRequest);
}
