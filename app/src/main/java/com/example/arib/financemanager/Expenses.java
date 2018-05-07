package com.example.arib.financemanager;

/**
 * Created by Arib on 12/4/2016.
 */

//class that defines what an expense is
class Expenses {

    //private variables that hold the definitive properties of an expense

    //the name of the expense
    private String title;

    //the category it belongs to
    private String category;

    //the amount the expense was
    private double amount;

    //the date the expense was added
    private String date;

    //default constructor that sets all the faults to default
    Expenses() {
        title = "";
        category = "";
        amount = 0.0;
        date = "";

    }

    //constructor that takes all of the data and stores into the object
    Expenses(String title, double amount, String category, String date) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

    //getter and setters
    String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    double getAmount() {
        return amount;
    }

    void setAmount(double amount) {
        this.amount = amount;
    }

    String getCategory() {
        return category;
    }

    void setCategory(String category) {
        this.category = category;
    }

    String getDate() {
        return date;
    }

    void setDate(String date) {
        this.date = date;
    }

    //returns the title then amount then category then date separated by '-' dashes
    public String toString() {
        return getTitle() + "-" + getAmount() + "-" + getCategory() + "-" + getDate();
    }
}
