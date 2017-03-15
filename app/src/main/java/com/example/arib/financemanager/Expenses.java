package com.example.arib.financemanager;

/**
 * Created by Arib on 12/4/2016.
 */

class Expenses {
    private String title;
    private String category;
    private double amount;
    private String date;

    Expenses() {
        title = "";
        category = "";
        amount = 0.0;
        date = "";

    }

    Expenses(String title, double amount, String category, String date) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

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

    public String toString() {
        return getTitle() + "-" + getAmount() + "-" + getCategory() + "-" + getDate();
    }
}
