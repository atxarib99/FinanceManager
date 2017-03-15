package com.example.arib.financemanager;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class PastDataActivity extends Activity {

    String textualExpenses;
    private ArrayList<Expenses> expenses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar bar = getActionBar();
        assert bar != null;
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#4E9455")));
        setContentView(R.layout.activity_main);
        textualExpenses = getIntent().getStringExtra("list");
        expenses = getExpenses();
        updateBalance();
        updateList();
    }

    public ArrayList<Expenses> getExpenses() {
        String finalString = textualExpenses.replace("[", "");
        finalString = finalString.replace("]", "");
        String[] arrayToConvert = finalString.split(", ");
        ArrayList<Expenses> returnable = new ArrayList<>();
        for(String s : arrayToConvert) {
            String[] indivExpense = s.split("-");
            String title = indivExpense[0];
            double amount = Double.parseDouble(indivExpense[1]);
            String category = indivExpense[2];
            String date = indivExpense[3];
            returnable.add(new Expenses(title, amount, category, date));
        }

        return returnable;
    }

    public void updateList() {
        PastDataActivity.MyListAdapter adapter = new PastDataActivity.MyListAdapter();
        ListView listView = (ListView) findViewById(R.id.expensesList);
        listView.setAdapter(adapter);
    }

    public void updateBalance() {
        double tempBalance = 0;
        for(Expenses e : expenses) {
            tempBalance += e.getAmount();
        }
        TextView balance = (TextView) findViewById(R.id.balance);
        //Locale.getDefault() get the location because letters vary in different regions of the world
        String stringBalance = String.format(Locale.getDefault(), "%.2f", tempBalance);
        balance.setText(stringBalance);
    }


    private class MyListAdapter extends ArrayAdapter<Expenses> {
        MyListAdapter() {
            super(PastDataActivity.this, R.layout.listview_item, expenses);
        }

        //get the view of the item
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            //get the item view
            View itemView = convertView;
            if(itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.listview_item, parent, false);
            }

            //get the values to set
            Expenses currentExpense = expenses.get(position);
            String title = currentExpense.getTitle();
            String amount = String.format(Locale.getDefault(), "%.2f", currentExpense.getAmount());
            amount = "$" + amount;

            //get the textviews
            TextView expensesTitle = (TextView) itemView.findViewById(R.id.expensetitle);
            TextView expensesAmount = (TextView) itemView.findViewById(R.id.expenseamount);


            //set the textviews to the values
            expensesTitle.setText(title);
            expensesAmount.setText(amount);

            //return the View as requested
            return itemView;

        }

    }
}
