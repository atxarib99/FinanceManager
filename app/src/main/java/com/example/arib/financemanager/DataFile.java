package com.example.arib.financemanager;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

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

    ArrayList<Expenses> getExpenses() {
        String finalString = textualExpenses.replace("[", "");
        finalString = finalString.replace("]", "");
        String[] arrayToConvert = finalString.split(", ");
        ArrayList<Expenses> returnable = new ArrayList<>();
        for(String s : arrayToConvert) {
            String[] indivExpense = s.split("-");
            Log.d(LOG_TAG, returnable + "");
            Log.d(LOG_TAG, Arrays.toString(indivExpense));
            String title = indivExpense[0];
            double amount = Double.parseDouble(indivExpense[1]);
            String category = indivExpense[2];
            String date = indivExpense[3];
            returnable.add(new Expenses(title, amount, category, date));
        }

        return returnable;
    }

    public String toString() {
        return textualExpenses;
    }
}
