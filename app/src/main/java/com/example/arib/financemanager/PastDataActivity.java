package com.example.arib.financemanager;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class PastDataActivity extends Activity {

    String textualExpenses;
    static ArrayList<Expenses> expenses;
    private final String LOG_TAG = PastDataActivity.class.getSimpleName();
    SmsManager smsManager;
    Expenses selectedExpense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        smsManager = SmsManager.getDefault();
        ActionBar bar = getActionBar();
        assert bar != null;
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#4E9455")));
        setContentView(R.layout.activity_main);
        textualExpenses = getIntent().getStringExtra("list");
        String monthName = getIntent().getStringExtra("month");
        TextView title = (TextView) findViewById(R.id.month);
        title.setText(monthName);
        expenses = getExpenses();
        setInfoListener();
        updateBalance();
        updateList();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_past, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_graphpast) {
            Intent intent = new Intent(this, GraphActivity.class);
            intent.putExtra("Total Balance Past", updateBalance());
            intent.putExtra("type", "Past");
            startActivity(intent);
        }
        if(id == R.id.action_invoicepast) {
            Intent smsIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(smsIntent, 27);
        }
        if(id == R.id.action_exit) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setInfoListener() {
        ListView listView = (ListView) findViewById(R.id.expensesList);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if(selectedExpense == null) return false;
                selectedExpense = expenses.get(position);
                getInfoDialog();
                return true;
            }
        });
    }

    public Dialog getInfoDialog() {
        Dialog builder = new Dialog(this);
        builder.setTitle("Info");
        builder.setContentView(R.layout.dialog_info);
        String title = selectedExpense.getTitle();
        String expense = String.format("%.2f", selectedExpense.getAmount()); //check locale
        expense = "$" + expense;
        String category = selectedExpense.getCategory();
        String date = selectedExpense.getDate();

        TextView titleView = (TextView) builder.findViewById(R.id.infodialog_title);
        TextView amountView = (TextView) builder.findViewById(R.id.infodialog_amount);
        TextView categoryView = (TextView) builder.findViewById(R.id.infodialog_category);
        TextView dateView = (TextView) builder.findViewById(R.id.infodialog_date);

        titleView.setText(title);
        amountView.setText(expense);
        categoryView.setText(category);
        dateView.setText(date);

        return builder;
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

    public double updateBalance() {
        double tempBalance = 0;
        for(Expenses e : expenses) {
            tempBalance += e.getAmount();
        }
        TextView balance = (TextView) findViewById(R.id.balance);
        //Locale.getDefault() get the location because letters vary in different regions of the world
        String stringBalance = String.format(Locale.getDefault(), "%.2f", tempBalance);
        balance.setText(stringBalance);
        return tempBalance;
    }
    public String getStringOfData() {
        String stringOfData;
        stringOfData = "Total Spent: $" + String.format(Locale.getDefault(), "%.2f", updateBalance()) + "\n\n";
        stringOfData += "By Category\n";
        int totalItems = PastDataActivity.expenses.size();
        ArrayList<String> numberOfCats = new ArrayList<>();
        for(int i = 0; i < totalItems; i++) {
            if(!(numberOfCats.contains(PastDataActivity.expenses.get(i).getCategory()))) {
                numberOfCats.add(PastDataActivity.expenses.get(i).getCategory());
            }
        }
        // how many of first category divided by total number
        for(int i = 0; i < numberOfCats.size(); i++) {
            double totalSpent = 0;
            for (int k = 0; k < totalItems; k++) {
                if (numberOfCats.get(i).equals(PastDataActivity.expenses.get(k).getCategory())) {
                    totalSpent += PastDataActivity.expenses.get(k).getAmount();
                }
            }
            stringOfData += numberOfCats.get(i) + " $" + String.format(Locale.getDefault(), "%.2f", totalSpent) + "\n";
        }

        stringOfData += "\n";
        stringOfData += "By Item\n";
        for(Expenses e : expenses) {
            stringOfData += e.getTitle() + " $" + String.format(Locale.getDefault(), "%.2f", e.getAmount()) + "\n";
        }
        Log.d(LOG_TAG, stringOfData);
        return stringOfData;
    }

    public String getMonth(int intMonth) {
        switch (intMonth) {
            case 1 : return "January";
            case 2 : return "February";
            case 3 : return "March";
            case 4 : return "April";
            case 5 : return "May";
            case 6 : return "June";
            case 7 : return "July";
            case 8 : return "August";
            case 9 : return "September";
            case 10 : return "October";
            case 11 : return "November";
            case 12 : return "December";
            default : return "lol garbo programmer";
        }
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        switch (reqCode) {

            case(27) :
                Calendar cal = Calendar.getInstance();
                if(resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    Cursor c = getContentResolver().query(contactData, null, null, null, null);
                    ContentResolver contactResolver = getContentResolver();
                    if(c.moveToFirst()) {
                        String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                        Cursor phoneCur = contactResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { id }, null);
                        if(phoneCur.moveToFirst()) {
                            String no = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            String smsBody = "Invoice of: " + getMonth(cal.get(Calendar.MONTH) + 1) + "\n\n";
                            smsBody += getStringOfData();
                            ArrayList<String> parts = smsManager.divideMessage(smsBody);
                            smsManager.sendMultipartTextMessage(no, null, parts, null, null);
                        }
                    }
                }
        }
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
