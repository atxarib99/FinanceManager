package com.arib.financemanager;

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

//Activity that shows the Graph screen
public class GraphActivity extends Activity {

    //Create a PieChart object
    private PieChart pieChart;

    //array that will hold the values and array that will hold the names
    private float[] values;
    private String[] names;

    //total spent to display
    double spent;

    //LOG TAG for this class
    private final String LOG_TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //create the intent object to retrieve the data
        Intent intent = getIntent();
        //get the 'type' string extra from the intent
        //the type string extra is used to know if the data we are loading is current or mast
        String fromWhere = intent.getStringExtra("type");

        //get the total value from the intent
        if(fromWhere.equals("Main"))
            spent = intent.getDoubleExtra("Total Balance Main", 0.0);
        else
            spent = intent.getDoubleExtra("Total Balance Past", 0.0);

        //get the action bar
        ActionBar bar = getActionBar();
        assert bar != null;
        //set its color to the application color
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#4E9455")));
        //set its title to say the string below
        bar.setTitle("This month's expenses");

        //set the content view
        setContentView(R.layout.activity_graph);

        //update the view for the amount spent
        updateBalance(spent);

        //like the piechart to the view element
        pieChart = (PieChart) findViewById(R.id.piechart);

        //get the items from the file
        if(fromWhere.equals("Main"))
            getItems();
        else
            getPastItems();

        //create a description object and disable it
        Description desc = new Description();
        desc.setEnabled(false);

        //give the description object to the pie chart
        pieChart.setDescription(desc);

        //create a hole in the pie chart
        pieChart.setDrawHoleEnabled(true);
        //set the colors hole to be transparent
        pieChart.setHoleColor(Color.TRANSPARENT);
        //the hole in the pie chart will have a dp of 15
        pieChart.setHoleRadius(15);
        //set the transparency to 10dp
        pieChart.setTransparentCircleRadius(10);

        //start with no rotation
        pieChart.setRotation(0);
        //allow rotation
        pieChart.setRotationEnabled(true);

        //add the data to the pie chart
        addData();

        //get the legend from the pie chart and disable it since the colors are arbitrary and do not have any significance
        Legend legend = pieChart.getLegend();
        legend.setEnabled(false);

    }

    //adds the data to the pie chart
    private void addData() {
        //create an array list of pieentry objects
        //A pieentry holds a value and a title
        ArrayList<PieEntry> yVals = new ArrayList<>();
        //for each item of values
        for (int i = 0; i < values.length; i++) {
            //add the current value and its corresponding title to the arraylist
            yVals.add(new PieEntry(values[i], names[i]));
        }

        //create an array of values for the dependent axis
        ArrayList<String> xVals = new ArrayList<>();
        //add all the names from the name list to the xVals array list
        xVals.addAll(Arrays.asList(names).subList(0, values.length));

        //Create a data set for the chart define the y values as the expenses
        PieDataSet dataSet = new PieDataSet(yVals, "Expenses");
        //default slice space and selection shift values
        dataSet.setSliceSpace(3);
        dataSet.setSelectionShift(5);

        //create a color palate list
        ArrayList<Integer> colors = new ArrayList<Integer>();

        //add each color from the material colors list
        for(int c : ColorTemplate.MATERIAL_COLORS)
            colors.add(c);

        //add a holo blue because I like it
        colors.add(ColorTemplate.getHoloBlue());
        //give the colors to the data set
        dataSet.setColors(colors);

        //Create a pie data from the dataset
        PieData data = new PieData(dataSet);
        //define formatter
        data.setValueFormatter(new PercentFormatter());
        //define size of text
        data.setValueTextSize(15f);
        //define the text color
        data.setValueTextColor(Color.BLACK);

        //set the data
        pieChart.setData(data);

        //no highlight for the selected value
        pieChart.highlightValue(null);
        //necessary command
        pieChart.invalidate();

    }

    //get the items from the main activity
    private void getItems() {
        //how many categories are there?
        int totalItems = MainActivity.expenses.size();
        //create a arraylist of the number of categories
        ArrayList<String> numberOfCats = new ArrayList<>();

        //for each item of the expenses
        for(int i = 0; i < totalItems; i++) {
            //if the category hasnt been added; add it
            if(!(numberOfCats.contains(MainActivity.expenses.get(i).getCategory()))) {
                numberOfCats.add(MainActivity.expenses.get(i).getCategory());
            }
        }

        //instantiates values and names arrays
        values = new float[numberOfCats.size()];
        names = new String[numberOfCats.size()];


        //for each category
        for(int i = 0; i < numberOfCats.size(); i++) {
            //holds how much is spent
            double totalSpent = 0;
            //for each item of expenses
            for(int k = 0; k < totalItems; k++) {
                //if the current item belongs to this category add its amount to total spent
                if(numberOfCats.get(i).equals(MainActivity.expenses.get(k).getCategory())) {
                    totalSpent += MainActivity.expenses.get(k).getAmount();
                }
            }

            //creates totalSpent as a formatted string
            String sTotalSpent = String.format(Locale.getDefault(), "%.2f", totalSpent);
            //stores the percentage for this category into values array
            values[i] = (float) (totalSpent / spent) * 100;
            //stores the name and how much is spent into names array
            names[i] = numberOfCats.get(i) + " $" + sTotalSpent;
        }

    }

    //Same process as getItems except pulls data from the PastDataActivity instead of MainActivity
    private void getPastItems() {
        //how many categories are there?
        int totalItems = PastDataActivity.expenses.size();
        ArrayList<String> numberOfCats = new ArrayList<>();
        for(int i = 0; i < totalItems; i++) {
            //loads from PastDataActivity instead of MainActivity
            if(!(numberOfCats.contains(PastDataActivity.expenses.get(i).getCategory()))) {
                numberOfCats.add(PastDataActivity.expenses.get(i).getCategory());
            }
        }

        values = new float[numberOfCats.size()];
        names = new String[numberOfCats.size()];

        for(int i = 0; i < numberOfCats.size(); i++) {
            double totalSpent = 0;
            for(int k = 0; k < totalItems; k++) {
                if(numberOfCats.get(i).equals(PastDataActivity.expenses.get(k).getCategory())) {
                    totalSpent += PastDataActivity.expenses.get(k).getAmount();
                }
            }

            String sTotalSpent = String.format(Locale.getDefault(), "%.2f", totalSpent);
            values[i] = (float) (totalSpent / spent) * 100;
            names[i] = numberOfCats.get(i) + " $" + sTotalSpent;
        }

    }

    //updates the balance textview
    private void updateBalance(double value) {
        //get the textview
        TextView balance = (TextView) findViewById(R.id.graph_balance);
        //get the balance as a string
        //Locale.getDefault() get the location because letters vary in different regions of the world
        String stringBalance = String.format(Locale.getDefault(), "%.2f", value);
        //set the balance to the textview with a '$'
        balance.setText("$" + stringBalance);
    }
}
