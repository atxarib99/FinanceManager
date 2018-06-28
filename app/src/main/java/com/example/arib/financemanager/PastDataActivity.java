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
import android.os.Vibrator;
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
import java.util.List;
import java.util.Locale;

//activity for the past data
public class PastDataActivity extends Activity {

    //holds the expenses as a string for easy passthrough
    String textualExpenses;
    //holds the expenses as a list of objects
    static ArrayList<Expenses> expenses;

    //log tag for the class
    private final String LOG_TAG = PastDataActivity.class.getSimpleName();

    //This class's smsmanager
    SmsManager smsManager;

    //long held item
    Expenses selectedExpense;

    //creates the view
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //gets the smsmanager
        smsManager = SmsManager.getDefault();
        //gets the action bar and sets the proper color
        ActionBar bar = getActionBar();
        assert bar != null;
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#4E9455")));
        //sets the view
        setContentView(R.layout.activity_main);
        //gets the expenses as a string from the intent
        textualExpenses = getIntent().getStringExtra("list");
        //gets the month name from the intent
        String monthName = getIntent().getStringExtra("month");
        //Creates textviews
        TextView title = (TextView) findViewById(R.id.month);
        //sets data
        title.setText(monthName);
        //gets expeneses as a list
        expenses = getExpenses();
        //sets long press listener
        setInfoListener();
        //updates views
        updateBalance();
        updateList();
    }

    //creates the options menu (modified from main)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_past, menu);
        //get title
        TextView title = (TextView) findViewById(R.id.month);
        return super.onCreateOptionsMenu(menu);
    }

    //defines what each option does
    public boolean onOptionsItemSelected(MenuItem item) {
        //gets the id
        int id = item.getItemId();

        //if graph is pressed
        if(id == R.id.action_graphpast) {
            //create intent to create activity
            Intent intent = new Intent(this, GraphActivity.class);
            //pass the balance
            intent.putExtra("Total Balance Past", updateBalance());
            //pass that the call came from the pastdataactivity class
            intent.putExtra("type", "Past");
            //start and go
            startActivity(intent);
        }
        //sends invoice to a contact
        if(id == R.id.action_invoicepast) {
            Intent smsIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(smsIntent, 27);
        }
        //if exit is pressed
        if(id == R.id.action_exit) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    //sets long item click listenter
    private void setInfoListener() {
        ListView listView = (ListView) findViewById(R.id.expensesList);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                selectedExpense = expenses.get(position);
                if(selectedExpense == null) return false;
                getInfoDialog().show();
                return true;
            }
        });
    }

    //creates and returns the Dialog that displays detailed information of the selected item
    public Dialog getInfoDialog() {
        //create a dialog object
        Dialog builder = new Dialog(this);
        //change its title
        builder.setTitle("Info");
        //give it a new view to display
        builder.setContentView(R.layout.dialog_info);
        //get the title of the expense
        String title = selectedExpense.getTitle();
        //get the amount of the expense and format it to 2 decimal places
        String expense = String.format("%.2f", selectedExpense.getAmount()); //check locale
        //add a preceding dollar sign
        expense = "$" + expense;
        //get the category the item belongs to
        String category = selectedExpense.getCategory();
        //get its creation date
        String date = selectedExpense.getDate();

        //Create and link textviews
        TextView titleView = (TextView) builder.findViewById(R.id.infodialog_title);
        TextView amountView = (TextView) builder.findViewById(R.id.infodialog_amount);
        TextView categoryView = (TextView) builder.findViewById(R.id.infodialog_category);
        TextView dateView = (TextView) builder.findViewById(R.id.infodialog_date);

        //set the data from the Expense to the textviews
        titleView.setText(title);
        amountView.setText(expense);
        categoryView.setText(category);
        dateView.setText(date);

        //return the Dialog
        return builder;
    }

    //get the list of expenses as a list from the defined global string
    public ArrayList<Expenses> getExpenses() {
        //replace the brackets
        String finalString = textualExpenses.replace("[", "");
        finalString = finalString.replace("]", "");
        //split on the commas
        String[] arrayToConvert = finalString.split(", ");
        //create list that will hold the expenese
        ArrayList<Expenses> returnable = new ArrayList<>();
        //for each string we have after splitting
        for(String s : arrayToConvert) {
            //split on dashes
            String[] indivExpense = s.split("-");
            //the order will be title-amount-category-date
            String title = indivExpense[0];
            double amount = Double.parseDouble(indivExpense[1]);
            String category = indivExpense[2];
            String date = indivExpense[3];
            //create the data into an Expenes object and add it to the list
            returnable.add(new Expenses(title, amount, category, date));
        }

        //return the arraylist
        return returnable;
    }

    //update the listview
    public void updateList() {
        PastDataActivity.MyListAdapter adapter = new PastDataActivity.MyListAdapter();
        ListView listView = (ListView) findViewById(R.id.expensesList);
        listView.setAdapter(adapter);
    }

    //calculate and update the balance
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

    //Get the returns the data as a string and formatted. Check MainActivity for how it is created.
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

    //returns string value of month from integer
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
            default : return "lol garbo programmer"; //hehe
        }
    }

    //On result of intent for result call
    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        switch (reqCode) {

            //send unencrypted data through SMS. Check MainActivity for detail. follows same procedure.
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

    //Custom List Adapter class
    private class MyListAdapter extends ArrayAdapter<Expenses> {
        //constructor
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
