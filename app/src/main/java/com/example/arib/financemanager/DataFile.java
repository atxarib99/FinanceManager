package com.example.arib.financemanager;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Arib on 12/12/2016.
 */

class DataFile implements Serializable {
    private String filename;
    private String textualExpenses;
    private final String LOG_TAG = this.getClass().getSimpleName();

    DataFile(String filename, ArrayList<Expenses> expenses) {
        this.filename = filename;
        textualExpenses = expenses.toString();
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String toString() {
        return textualExpenses;
    }
}
