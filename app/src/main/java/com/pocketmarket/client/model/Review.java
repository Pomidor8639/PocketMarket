package com.pocketmarket.client.model;

import java.io.Serializable;

public class Review implements Serializable {
    private String user;
    private double rating;
    private String comment;
    private String date;

    public Review() {
    }

    public Review(String user, double rating, String comment, String date) {
        this.user = user;
        this.rating = rating;
        this.comment = comment;
        this.date = date;
    }

    public String getUser() {
        return user != null ? user : "Steve";
    }

    public void setUser(String user) {
        this.user = user;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment != null ? comment : "";
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDate() {
        return date != null ? date : "";
    }

    public void setDate(String date) {
        this.date = date;
    }
}
