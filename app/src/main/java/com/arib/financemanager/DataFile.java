package com.arib.financemanager;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Arib on 12/12/2016.
 * This file is used hold the expenses as a data object
 * It turns data from an array of Expenses.java object to a string format that can be easily stored
 * and restored back into an array
 */

class DataFile implements Serializable {
    //name of the file; used to differentiate between months of data
    private String filename;

    //string of data
    private String textualExpenses;

    //LOG TAG for this class
    private final String LOG_TAG = this.getClass().getSimpleName();

    //Constructor that takes a filename and a list of Expenses
    DataFile(String filename, ArrayList<Expenses> expenses) {
        //set the filename to given
        this.filename = filename;
        //get the array's to string and store that into data
        textualExpenses = expenses.toString();
    }

    //getter and setter for filename
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    //returns the data
    public String toString() {
        return textualExpenses;
    }
}
