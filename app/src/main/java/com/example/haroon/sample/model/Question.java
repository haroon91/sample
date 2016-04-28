package com.example.haroon.sample.model;

/**
 * Created by Haroon on 4/3/2016.
 */
public class Question {

    public int questionId;
    public String description;
    public String status;
    public int userId;
    public int answeredTutorId;

    public String creationDate;
    public double userRating;

    public Subject subject;

    public User askedBy;
    public User answeredBy;

    public String pictureUrl; //image posted with the question
}
