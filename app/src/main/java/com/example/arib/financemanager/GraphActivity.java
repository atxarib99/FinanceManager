package com.example.arib.financemanager;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Display;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class GraphActivity extends Activity {

    private PieChart pieChart;

    private float[] values;
    private String[] names;

    double spent;

    private final String LOG_TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String fromWhere = intent.getStringExtra("type");
        double value;

        if(fromWhere.equals("Main"))
            value = intent.getDoubleExtra("Total Balance Main", 0.0);
        else
            value = intent.getDoubleExtra("Total Balance Past", 0.0);

        spent = value;
        ActionBar bar = getActionBar();
        assert bar != null;
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#4E9455")));
        bar.setTitle("This month's expenses");
        setContentView(R.layout.activity_graph);
        updateBalance(value);
        pieChart = (PieChart) findViewById(R.id.piechart);

        Description desc = new Description();
        desc.setEnabled(false);

        if(fromWhere.equals("Main"))
            getItems();
        else
            getPastItems();
        pieChart.setDescription(desc);


        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(15);
        pieChart.setTransparentCircleRadius(10);

        pieChart.setRotation(0);
        pieChart.setRotationEnabled(true);

        addData();

        Legend legend = pieChart.getLegend();
        legend.setEnabled(false);

    }

    private void addData() {
        ArrayList<PieEntry> yVals = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            yVals.add(new PieEntry(values[i], names[i]));
        }

        ArrayList<String> xVals = new ArrayList<>();
        xVals.addAll(Arrays.asList(names).subList(0, values.length));

        PieDataSet dataSet = new PieDataSet(yVals, "Expenses");
        dataSet.setSliceSpace(3);
        dataSet.setSelectionShift(5);

        ArrayList<Integer> colors = new ArrayList<Integer>();

        for(int c : ColorTemplate.MATERIAL_COLORS)
            colors.add(c);

        colors.add(ColorTemplate.getHoloBlue());
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(15f);
        data.setValueTextColor(Color.BLACK);

        pieChart.setData(data);

        pieChart.highlightValue(null);

        pieChart.invalidate();

    }

    private void getItems() {
        //how many categories are there?
        int totalItems = MainActivity.expenses.size();
        ArrayList<String> numberOfCats = new ArrayList<>();
        for(int i = 0; i < totalItems; i++) {
            if(!(numberOfCats.contains(MainActivity.expenses.get(i).getCategory()))) {
                numberOfCats.add(MainActivity.expenses.get(i).getCategory());
            }
        }
        Log.d(LOG_TAG, numberOfCats.toString());
        values = new float[numberOfCats.size()];
        names = new String[numberOfCats.size()];
        // how many of first category divided by total number
        double totalItemsd = (double) totalItems;
        for(int i = 0; i < numberOfCats.size(); i++) {
            double thisCatAmount = 0;
            double totalSpent = 0;
            for(int k = 0; k < totalItems; k++) {
                if(numberOfCats.get(i).equals(MainActivity.expenses.get(k).getCategory())) {
                    thisCatAmount++;
                    totalSpent += MainActivity.expenses.get(k).getAmount();
                }
            }

            String sTotalSpent = String.format(Locale.getDefault(), "%.2f", totalSpent);
            Log.d(LOG_TAG, thisCatAmount + "--" + totalItemsd);
            Log.d(LOG_TAG, (thisCatAmount/totalItems) * 100.0 + "");
            values[i] = (float) (totalSpent / spent) * 100;
            names[i] = numberOfCats.get(i) + " $" + sTotalSpent;
            Log.d(LOG_TAG, Arrays.toString(values));
            Log.d(LOG_TAG, Arrays.toString(names));
        }

    }

    private void getPastItems() {
        //how many categories are there?
        int totalItems = PastDataActivity.expenses.size();
        ArrayList<String> numberOfCats = new ArrayList<>();
        for(int i = 0; i < totalItems; i++) {
            if(!(numberOfCats.contains(PastDataActivity.expenses.get(i).getCategory()))) {
                numberOfCats.add(PastDataActivity.expenses.get(i).getCategory());
            }
        }
        Log.d(LOG_TAG, numberOfCats.toString());
        values = new float[numberOfCats.size()];
        names = new String[numberOfCats.size()];
        // how many of first category divided by total number
        double totalItemsd = (double) totalItems;
        for(int i = 0; i < numberOfCats.size(); i++) {
            double thisCatAmount = 0;
            double totalSpent = 0;
            for(int k = 0; k < totalItems; k++) {
                if(numberOfCats.get(i).equals(PastDataActivity.expenses.get(k).getCategory())) {
                    thisCatAmount++;
                    totalSpent += PastDataActivity.expenses.get(k).getAmount();
                }
            }

            String sTotalSpent = String.format(Locale.getDefault(), "%.2f", totalSpent);
            Log.d(LOG_TAG, thisCatAmount + "--" + totalItemsd);
            Log.d(LOG_TAG, (thisCatAmount/totalItems) * 100.0 + "");
            values[i] = (float) (totalSpent / spent) * 100;
            names[i] = numberOfCats.get(i) + " $" + sTotalSpent;
            Log.d(LOG_TAG, Arrays.toString(values));
            Log.d(LOG_TAG, Arrays.toString(names));
        }

    }


    private void updateBalance(double value) {
        TextView balance = (TextView) findViewById(R.id.graph_balance);
        //Locale.getDefault() get the location because letters vary in different regions of the world
        String stringBalance = String.format(Locale.getDefault(), "%.2f", value);
        balance.setText("$" + stringBalance);
    }
}
